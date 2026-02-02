package com.shejan.nextdo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;

import com.shejan.nextdo.databinding.ActivityNewTaskBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// DEFINITIVE FIX: Correctly managing the unique alarmId for every task.
public class NewTaskActivity extends AppCompatActivity {
    private static final String TAG = "NewTaskActivity";

    public static final String EXTRA_ID = "com.shejan.nextdo.ID";
    public static final String EXTRA_ALARM_ID = "com.shejan.nextdo.ALARM_ID";
    public static final String EXTRA_TITLE = "com.shejan.nextdo.TITLE";
    public static final String EXTRA_DESCRIPTION = "com.shejan.nextdo.DESCRIPTION";

    public static final String EXTRA_REMINDER_TIME = "com.shejan.nextdo.REMINDER_TIME";
    public static final String EXTRA_REPEAT = "com.shejan.nextdo.REPEAT";
    public static final String EXTRA_REMINDER_TYPE = "com.shejan.nextdo.REMINDER_TYPE";
    public static final int RESULT_DELETE = 2;

    private ActivityNewTaskBinding binding;
    private final Calendar calendar = Calendar.getInstance();
    private int taskId = 0;
    private int alarmId = 0;
    private boolean isReminderSet = false;
    private AlarmScheduler alarmScheduler;
    private String selectedRepeatDays = "";
    private String selectedReminderType = "notification";

    // Voice preview components
    private LinearLayout voicePreviewBox;
    private ImageView btnPlayVoice;
    private EqualizerView voiceEqualizer;
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean isSpeaking = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityNewTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Remove toolbar setup for Nothing theme

        alarmScheduler = new AlarmScheduler(this);

        // Setup Repeat Dropdown
        binding.textRepeat.setOnClickListener(v -> showWeekdayRepeatDialog());

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Todo");
            }
            taskId = intent.getIntExtra(EXTRA_ID, 0);
            alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 0);
            String title = intent.getStringExtra(EXTRA_TITLE);
            String description = intent.getStringExtra(EXTRA_DESCRIPTION);

            binding.editTitle.setText(title != null ? title : "");
            binding.editDescription.setText(description != null ? description : "");

            String repeat = intent.getStringExtra(EXTRA_REPEAT);
            if (repeat != null) {
                selectedRepeatDays = repeat;
                updateRepeatTextFromSelection();
            }

            long reminderTime = intent.getLongExtra(EXTRA_REMINDER_TIME, 0);
            if (reminderTime > 0) {
                calendar.setTimeInMillis(reminderTime);
                isReminderSet = true;
                updateReminderTimeText();
            }

            // Load reminder type
            String reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE);
            if (reminderType != null) {
                selectedReminderType = reminderType;
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Todo");
            }
        }

        binding.buttonSetReminder.setOnClickListener(v -> showDateTimePicker());

        setupBackButton();

        // Setup Reminder Type Selection (Radio button behavior)
        setupReminderTypeSelection();

        // Restore reminder type selection state (must be after setup)
        restoreReminderTypeSelection();

        // Setup Voice Preview
        setupVoicePreview();

        binding.buttonSave.setOnClickListener(view -> {
            try {
                Intent replyIntent = new Intent();
                if (TextUtils.isEmpty(binding.editTitle.getText())) {
                    setResult(RESULT_CANCELED, replyIntent);
                } else {
                    String title = binding.editTitle.getText().toString();
                    String description = binding.editDescription.getText().toString();

                    String repeat = selectedRepeatDays;
                    long reminderTime = isReminderSet ? calendar.getTimeInMillis() : 0;

                    Task task = new Task();
                    if (taskId != 0) {
                        task.id = taskId;
                    }
                    if (alarmId == 0 && reminderTime > System.currentTimeMillis()) {
                        alarmId = (int) System.currentTimeMillis();
                    }
                    task.alarmId = alarmId;
                    task.title = title;
                    task.description = description;

                    task.reminderTime = reminderTime;
                    task.repeat = repeat;
                    task.reminderType = selectedReminderType;

                    Log.d(TAG, "SAVE DEBUG: selectedReminderType = " + selectedReminderType);
                    Log.d(TAG, "SAVE DEBUG: task.reminderType = " + task.reminderType);

                    Log.d(TAG, "Task details: id=" + task.id + ", alarmId=" + task.alarmId +
                            ", reminderTime=" + reminderTime);

                    // NOTE: Do NOT schedule alarm here! MainActivity will schedule after database
                    // insert completes.
                    // Scheduling here causes double scheduling and race conditions.
                    Log.d(TAG, "NewTaskActivity: Not scheduling alarm here (will be scheduled by MainActivity)");

                    replyIntent.putExtra(EXTRA_ID, task.id);
                    replyIntent.putExtra(EXTRA_ALARM_ID, task.alarmId);
                    replyIntent.putExtra(EXTRA_TITLE, title);
                    replyIntent.putExtra(EXTRA_DESCRIPTION, description);

                    replyIntent.putExtra(EXTRA_REMINDER_TIME, reminderTime);
                    replyIntent.putExtra(EXTRA_REPEAT, repeat);
                    replyIntent.putExtra(EXTRA_REMINDER_TYPE, selectedReminderType);

                    Log.d(TAG, "SAVE DEBUG: Putting EXTRA_REMINDER_TYPE = " + selectedReminderType);

                    setResult(RESULT_OK, replyIntent);
                }
            } catch (Exception e) {
                setResult(RESULT_CANCELED, new Intent());
            }
            finish();
        });
    }

    private void showWeekdayRepeatDialog() {
        applyBlurEffect(true);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        // Inflate custom view
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_repeat_weekdays, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            // Side-Sheet Behavior
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;

            // Use 25% of screen width for compact side-sheet
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            params.width = (int) (displayMetrics.widthPixels * 0.25);
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            // Add some margin from the edge
            params.x = 32; // 32px margin from right

            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setDimAmount(0.3f); // Subtle dim
        }

        // Checkboxes
        android.widget.CheckBox cbSun = dialogView.findViewById(R.id.checkbox_sun);
        android.widget.CheckBox cbMon = dialogView.findViewById(R.id.checkbox_mon);
        android.widget.CheckBox cbTue = dialogView.findViewById(R.id.checkbox_tue);
        android.widget.CheckBox cbWed = dialogView.findViewById(R.id.checkbox_wed);
        android.widget.CheckBox cbThu = dialogView.findViewById(R.id.checkbox_thu);
        android.widget.CheckBox cbFri = dialogView.findViewById(R.id.checkbox_fri);
        android.widget.CheckBox cbSat = dialogView.findViewById(R.id.checkbox_sat);

        // Pre-select
        if (!TextUtils.isEmpty(selectedRepeatDays)) {
            String[] parts = selectedRepeatDays.split(",");
            for (String day : parts) {
                try {
                    int dayInt = Integer.parseInt(day.trim());
                    if (dayInt == Calendar.SUNDAY)
                        cbSun.setChecked(true);
                    else if (dayInt == Calendar.MONDAY)
                        cbMon.setChecked(true);
                    else if (dayInt == Calendar.TUESDAY)
                        cbTue.setChecked(true);
                    else if (dayInt == Calendar.WEDNESDAY)
                        cbWed.setChecked(true);
                    else if (dayInt == Calendar.THURSDAY)
                        cbThu.setChecked(true);
                    else if (dayInt == Calendar.FRIDAY)
                        cbFri.setChecked(true);
                    else if (dayInt == Calendar.SATURDAY)
                        cbSat.setChecked(true);
                } catch (NumberFormatException e) {
                    // Ignore legacy or malformed
                }
            }
        }

        // Select All Logic
        android.widget.CheckBox cbSelectAll = dialogView.findViewById(R.id.checkbox_select_all);

        // Listener for Select All
        cbSelectAll.setOnClickListener(v -> {
            boolean isChecked = cbSelectAll.isChecked();
            cbSun.setChecked(isChecked);
            cbMon.setChecked(isChecked);
            cbTue.setChecked(isChecked);
            cbWed.setChecked(isChecked);
            cbThu.setChecked(isChecked);
            cbFri.setChecked(isChecked);
            cbSat.setChecked(isChecked);
        });

        // Listeners for individual checkboxes to update Select All
        android.widget.CompoundButton.OnCheckedChangeListener individualListener = (buttonView, isChecked) -> {
            if (!isChecked) {
                cbSelectAll.setChecked(false);
            } else {
                if (cbSun.isChecked() && cbMon.isChecked() && cbTue.isChecked() &&
                        cbWed.isChecked() && cbThu.isChecked() && cbFri.isChecked() && cbSat.isChecked()) {
                    cbSelectAll.setChecked(true);
                }
            }
        };

        cbSun.setOnCheckedChangeListener(individualListener);
        cbMon.setOnCheckedChangeListener(individualListener);
        cbTue.setOnCheckedChangeListener(individualListener);
        cbWed.setOnCheckedChangeListener(individualListener);
        cbThu.setOnCheckedChangeListener(individualListener);
        cbFri.setOnCheckedChangeListener(individualListener);
        cbSat.setOnCheckedChangeListener(individualListener);

        // Initial state for Select All
        if (cbSun.isChecked() && cbMon.isChecked() && cbTue.isChecked() &&
                cbWed.isChecked() && cbThu.isChecked() && cbFri.isChecked() && cbSat.isChecked()) {
            cbSelectAll.setChecked(true);
        }

        // Listeners
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_done).setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            if (cbSun.isChecked())
                sb.append(Calendar.SUNDAY).append(",");
            if (cbMon.isChecked())
                sb.append(Calendar.MONDAY).append(",");
            if (cbTue.isChecked())
                sb.append(Calendar.TUESDAY).append(",");
            if (cbWed.isChecked())
                sb.append(Calendar.WEDNESDAY).append(",");
            if (cbThu.isChecked())
                sb.append(Calendar.THURSDAY).append(",");
            if (cbFri.isChecked())
                sb.append(Calendar.FRIDAY).append(",");
            if (cbSat.isChecked())
                sb.append(Calendar.SATURDAY).append(",");

            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1); // remove last comma
            }

            selectedRepeatDays = sb.toString();
            updateRepeatTextFromSelection();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> applyBlurEffect(false));
        dialog.show();

        // IMPORTANT: Set window parameters AFTER show() so window exists
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;

            // Use 35% of screen width for compact side-sheet
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            params.width = (int) (displayMetrics.widthPixels * 0.35);
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.x = 32; // 32px margin from right

            dialog.getWindow().setAttributes(params);
        }
    }

    private void updateRepeatTextFromSelection() {
        if (TextUtils.isEmpty(selectedRepeatDays)) {
            binding.textRepeat.setText("NONE");
            return;
        }

        StringBuilder display = new StringBuilder();
        String[] parts = selectedRepeatDays.split(",");
        int count = 0;
        for (String day : parts) {
            try {
                int d = Integer.parseInt(day.trim());
                if (d == Calendar.SUNDAY)
                    display.append("Sun, ");
                else if (d == Calendar.MONDAY)
                    display.append("Mon, ");
                else if (d == Calendar.TUESDAY)
                    display.append("Tue, ");
                else if (d == Calendar.WEDNESDAY)
                    display.append("Wed, ");
                else if (d == Calendar.THURSDAY)
                    display.append("Thu, ");
                else if (d == Calendar.FRIDAY)
                    display.append("Fri, ");
                else if (d == Calendar.SATURDAY)
                    display.append("Sat, ");
                count++;
            } catch (Exception e) {
            }
        }

        if (display.length() > 0) {
            display.setLength(display.length() - 2); // remove trailing ", "
        }

        if (count == 7)
            binding.textRepeat.setText("Everyday");
        else
            binding.textRepeat.setText(display.toString());
    }

    private void applyBlurEffect(boolean apply) {

        if (apply) {
            binding.blurOverlay.setVisibility(android.view.View.VISIBLE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                binding.rootLayout.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(
                                10f, 10f, android.graphics.Shader.TileMode.CLAMP));
            }
        } else {
            binding.blurOverlay.setVisibility(android.view.View.GONE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                binding.rootLayout.setRenderEffect(null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (taskId != 0) {
            getMenuInflater().inflate(R.menu.menu_edit_task, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            Intent replyIntent = new Intent();
            if (taskId != 0) {
                Task task = new Task();
                task.id = taskId;
                task.alarmId = alarmId;
                alarmScheduler.cancel(task);
                replyIntent.putExtra(EXTRA_ID, taskId);
            }
            setResult(RESULT_DELETE, replyIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showDateTimePicker() {
        long selection = isReminderSet ? calendar.getTimeInMillis()
                : com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds();
        ModernCalendarBottomSheet calendarSheet = ModernCalendarBottomSheet.newInstance(selection);
        calendarSheet.setOnDateSelectedListener(dateInMillis -> {
            // The custom calendar returns the selected date in local time (or whatever was
            // set in the calendar instance)
            // We need to update our local calendar with the Year, Month, Day from the
            // selection
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTimeInMillis(dateInMillis);

            calendar.set(Calendar.YEAR, selectedCal.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH));

            showTimePicker();
        });
        calendarSheet.show(getSupportFragmentManager(), "MODERN_CALENDAR");
    }

    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        com.google.android.material.timepicker.MaterialTimePicker timePicker = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Select Reminder Time")
                .setTheme(R.style.ThemeOverlay_App_MaterialTimePicker)
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            calendar.set(Calendar.SECOND, 0);
            isReminderSet = true;
            updateReminderTimeText();
        });

        // Position time picker at bottom like calendar
        timePicker.addOnDismissListener(dialog -> {
            // Cleanup if needed
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");

        // Position dialog at bottom after it's shown
        getSupportFragmentManager().executePendingTransactions();
        android.app.Dialog dialog = timePicker.getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            android.view.Window window = dialog.getWindow();
            window.setGravity(android.view.Gravity.BOTTOM);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.y = 0; // No offset from bottom
            window.setAttributes(params);
        }
    }

    private void updateReminderTimeText() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
            binding.textReminderTime.setText(sdf.format(calendar.getTime()));
        } catch (Exception e) {
            // Handle date formatting failure
        }
    }

    private void setupBackButton() {
        try {
            findViewById(R.id.back_arrow).setOnClickListener(v -> finish());
        } catch (Exception e) {
            // Handle back button setup failure
        }
    }

    private void setupReminderTypeSelection() {
        // Radio button behavior - only one can be selected at a time
        CompoundButton.OnCheckedChangeListener typeListener = (buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck others
                if (buttonView.getId() == R.id.checkbox_type_notification) {
                    binding.checkboxTypeAlarm.setChecked(false);
                    binding.checkboxTypeVoice.setChecked(false);
                    selectedReminderType = "notification";
                    if (voicePreviewBox != null) {
                        voicePreviewBox.setVisibility(android.view.View.GONE);
                        stopVoiceIconAnimation();
                    }
                } else if (buttonView.getId() == R.id.checkbox_type_alarm) {
                    binding.checkboxTypeNotification.setChecked(false);
                    binding.checkboxTypeVoice.setChecked(false);
                    selectedReminderType = "alarm";
                    if (voicePreviewBox != null) {
                        voicePreviewBox.setVisibility(android.view.View.GONE);
                        stopVoiceIconAnimation();
                    }
                } else if (buttonView.getId() == R.id.checkbox_type_voice) {
                    binding.checkboxTypeNotification.setChecked(false);
                    binding.checkboxTypeAlarm.setChecked(false);
                    selectedReminderType = "voice";
                    if (voicePreviewBox != null) {
                        voicePreviewBox.setVisibility(android.view.View.VISIBLE);
                        startVoiceIconAnimation();

                        // Check if male voice is available and prompt if needed
                        if (!TtsHelper.isPromptDismissed(NewTaskActivity.this)) {
                            TtsHelper.checkMaleVoiceAvailability(NewTaskActivity.this, hasMaleVoice -> {
                                if (!hasMaleVoice && !TtsHelper.isGoogleTtsInstalled(NewTaskActivity.this)) {
                                    runOnUiThread(() -> TtsHelper.promptInstallTts(NewTaskActivity.this));
                                }
                            });
                        }
                    }
                }
            } else {
                // Prevent unchecking all - at least one must be selected
                if (!binding.checkboxTypeNotification.isChecked() &&
                        !binding.checkboxTypeAlarm.isChecked() &&
                        !binding.checkboxTypeVoice.isChecked()) {
                    buttonView.setChecked(true);
                }
            }
        };

        binding.checkboxTypeNotification.setOnCheckedChangeListener(typeListener);
        binding.checkboxTypeAlarm.setOnCheckedChangeListener(typeListener);
        binding.checkboxTypeVoice.setOnCheckedChangeListener(typeListener);
    }

    private void restoreReminderTypeSelection() {
        // Update UI to match the loaded reminderType
        switch (selectedReminderType) {
            case "alarm":
                binding.checkboxTypeAlarm.setChecked(true);
                binding.checkboxTypeNotification.setChecked(false);
                binding.checkboxTypeVoice.setChecked(false);
                break;
            case "voice":
                binding.checkboxTypeVoice.setChecked(true);
                binding.checkboxTypeNotification.setChecked(false);
                binding.checkboxTypeAlarm.setChecked(false);
                break;
            case "notification":
            default:
                binding.checkboxTypeNotification.setChecked(true);
                binding.checkboxTypeAlarm.setChecked(false);
                binding.checkboxTypeVoice.setChecked(false);
                break;
        }
    }

    private void setupVoicePreview() {
        voicePreviewBox = binding.voicePreviewBox;
        btnPlayVoice = binding.btnPlayVoice;
        voiceEqualizer = binding.voiceEqualizer;

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED);

                if (isTtsReady) {
                    // Try to set male voice
                    setMaleVoice();
                }
            }
        });

        btnPlayVoice.setOnClickListener(v -> playVoicePreview());

        // Show preview if voice is already selected (when editing)
        if ("voice".equals(selectedReminderType)) {
            voicePreviewBox.setVisibility(android.view.View.VISIBLE);
            startVoiceIconAnimation();
        }
    }

    private void playVoicePreview() {
        String title = binding.editTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isTtsReady) {
            Toast.makeText(this, "Text-to-speech not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
            btnPlayVoice.setImageResource(R.drawable.ic_play);
        } else {
            String textToSpeak = "Reminder: " + title;
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "preview");
            isSpeaking = true;
            btnPlayVoice.setImageResource(R.drawable.ic_stop);

            // Reset button after speaking
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isSpeaking = false;
                btnPlayVoice.setImageResource(R.drawable.ic_play);
            }, 3000);
        }
    }

    private void startVoiceIconAnimation() {
        if (voiceEqualizer != null) {
            voiceEqualizer.startAnimation();

        }
    }

    private void stopVoiceIconAnimation() {
        if (voiceEqualizer != null) {
            voiceEqualizer.stopAnimation();
        }
    }

    private void setMaleVoice() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                java.util.Set<android.speech.tts.Voice> voices = textToSpeech.getVoices();
                if (voices != null) {
                    for (android.speech.tts.Voice voice : voices) {
                        // Look for English (US) Male voice
                        if (voice.getLocale().equals(Locale.US)) {
                            String voiceName = voice.getName().toLowerCase();
                            // Check if it's a male voice
                            if (voiceName.contains("male") && !voiceName.contains("female")) {
                                textToSpeech.setVoice(voice);
                                Log.d(TAG, "Selected male voice: " + voice.getName());
                                Toast.makeText(this, "Using voice: " + voice.getName(), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }

                    // Fallback: try any English male voice
                    for (android.speech.tts.Voice voice : voices) {
                        if (voice.getLocale().getLanguage().equals("en")) {
                            String voiceName = voice.getName().toLowerCase();
                            if (voiceName.contains("male") && !voiceName.contains("female")) {
                                textToSpeech.setVoice(voice);
                                Log.d(TAG, "Selected fallback male voice: " + voice.getName());
                                Toast.makeText(this, "Using voice: " + voice.getName(), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting male voice: " + e.getMessage());
                Toast.makeText(this, "Could not set male voice: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Male voice selection not available on this Android version", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        stopVoiceIconAnimation();
    }
}
