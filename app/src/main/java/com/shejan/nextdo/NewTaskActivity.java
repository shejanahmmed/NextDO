package com.shejan.nextdo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
    public static final String EXTRA_PRIORITY = "com.shejan.nextdo.PRIORITY";
    public static final String EXTRA_REMINDER_TIME = "com.shejan.nextdo.REMINDER_TIME";
    public static final String EXTRA_REPEAT = "com.shejan.nextdo.REPEAT";
    public static final int RESULT_DELETE = 2;

    private ActivityNewTaskBinding binding;
    private final Calendar calendar = Calendar.getInstance();
    private int taskId = 0;
    private int alarmId = 0;
    private boolean isReminderSet = false;
    private AlarmScheduler alarmScheduler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityNewTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Remove toolbar setup for Nothing theme

        alarmScheduler = new AlarmScheduler(this);

        // Setup Priority Dropdown
        binding.textPriority.setOnClickListener(v -> showPriorityOptions());

        // Setup Repeat Dropdown
        binding.textRepeat.setOnClickListener(v -> showRepeatOptions());

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

            String priority = intent.getStringExtra(EXTRA_PRIORITY);
            if (priority != null) {
                binding.textPriority.setText(priority);
            }

            String repeat = intent.getStringExtra(EXTRA_REPEAT);
            if (repeat != null) {
                binding.textRepeat.setText(repeat);
            }

            long reminderTime = intent.getLongExtra(EXTRA_REMINDER_TIME, 0);
            if (reminderTime > 0) {
                calendar.setTimeInMillis(reminderTime);
                isReminderSet = true;
                updateReminderTimeText();
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Todo");
            }
        }

        binding.buttonSetReminder.setOnClickListener(v -> showDateTimePicker());

        setupBackButton();

        binding.buttonSave.setOnClickListener(view -> {
            try {
                Intent replyIntent = new Intent();
                if (TextUtils.isEmpty(binding.editTitle.getText())) {
                    setResult(RESULT_CANCELED, replyIntent);
                } else {
                    String title = binding.editTitle.getText().toString();
                    String description = binding.editDescription.getText().toString();
                    String priority = binding.textPriority.getText().toString();
                    String repeat = binding.textRepeat.getText().toString();
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
                    task.priority = priority;
                    task.reminderTime = reminderTime;
                    task.repeat = repeat;

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
                    replyIntent.putExtra(EXTRA_PRIORITY, priority);
                    replyIntent.putExtra(EXTRA_REMINDER_TIME, reminderTime);
                    replyIntent.putExtra(EXTRA_REPEAT, repeat);

                    setResult(RESULT_OK, replyIntent);
                }
            } catch (Exception e) {
                setResult(RESULT_CANCELED, new Intent());
            }
            finish();
        });
    }

    private void showPriorityOptions() {
        applyBlurEffect(true);
        android.widget.ListPopupWindow listPopupWindow = new android.widget.ListPopupWindow(this);
        listPopupWindow.setAnchorView(binding.textPriority);
        listPopupWindow
                .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Set width to 150dp
        float density = getResources().getDisplayMetrics().density;
        listPopupWindow.setWidth((int) (150 * density));

        String[] options = getResources().getStringArray(R.array.priority_array);
        CardSpinnerAdapter adapter = new CardSpinnerAdapter(this, options);
        listPopupWindow.setAdapter(adapter);

        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            binding.textPriority.setText(options[position]);
            listPopupWindow.dismiss();
        });

        listPopupWindow.setOnDismissListener(() -> applyBlurEffect(false));
        listPopupWindow.show();
    }

    private void showRepeatOptions() {
        applyBlurEffect(true);
        android.widget.ListPopupWindow listPopupWindow = new android.widget.ListPopupWindow(this);
        listPopupWindow.setAnchorView(binding.textRepeat);
        listPopupWindow
                .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Set width to 150dp
        float density = getResources().getDisplayMetrics().density;
        listPopupWindow.setWidth((int) (150 * density));

        String[] options = getResources().getStringArray(R.array.repeat_array);
        CardSpinnerAdapter adapter = new CardSpinnerAdapter(this, options);
        listPopupWindow.setAdapter(adapter);

        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            binding.textRepeat.setText(options[position]);
            listPopupWindow.dismiss();
        });

        listPopupWindow.setOnDismissListener(() -> applyBlurEffect(false));
        listPopupWindow.show();
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

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
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
}
