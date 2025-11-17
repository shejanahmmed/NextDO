package com.shejan.nextdo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

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
    private Calendar calendar = Calendar.getInstance();
    private int taskId = 0;
    private int alarmId = 0;
    private AlarmScheduler alarmScheduler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityNewTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Remove toolbar setup for Nothing theme

        alarmScheduler = new AlarmScheduler(this);

        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.priority_array));
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (binding.spinnerPriority != null) {
            binding.spinnerPriority.setAdapter(priorityAdapter);
        }

        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.repeat_array));
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (binding.spinnerRepeat != null) {
            binding.spinnerRepeat.setAdapter(repeatAdapter);
        }

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
            if (priority != null && binding.spinnerPriority != null) {
                int priorityPosition = priorityAdapter.getPosition(priority);
                if (priorityPosition >= 0) {
                    binding.spinnerPriority.setSelection(priorityPosition);
                }
            }

            String repeat = intent.getStringExtra(EXTRA_REPEAT);
            if (repeat != null && binding.spinnerRepeat != null) {
                int repeatPosition = repeatAdapter.getPosition(repeat);
                if (repeatPosition >= 0) {
                    binding.spinnerRepeat.setSelection(repeatPosition);
                }
            }

            long reminderTime = intent.getLongExtra(EXTRA_REMINDER_TIME, 0);
            if (reminderTime > 0) {
                calendar.setTimeInMillis(reminderTime);
                updateReminderTimeText();
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Todo");
            }
        }

        if (binding.buttonSetReminder != null) {
            binding.buttonSetReminder.setOnClickListener(v -> showDateTimePicker());
        }

        setupBackButton();

        if (binding.buttonSave != null) {
            binding.buttonSave.setOnClickListener(view -> {
                try {
                    Intent replyIntent = new Intent();
                    if (binding.editTitle == null || TextUtils.isEmpty(binding.editTitle.getText())) {
                        setResult(RESULT_CANCELED, replyIntent);
                    } else {
                        String title = binding.editTitle.getText().toString();
                        String description = binding.editDescription != null
                                ? binding.editDescription.getText().toString()
                                : "";
                        String priority = binding.spinnerPriority != null
                                && binding.spinnerPriority.getSelectedItem() != null
                                        ? binding.spinnerPriority.getSelectedItem().toString()
                                        : "NONE";
                        String repeat = binding.spinnerRepeat != null && binding.spinnerRepeat.getSelectedItem() != null
                                ? binding.spinnerRepeat.getSelectedItem().toString()
                                : "NONE";
                        long reminderTime = calendar.getTimeInMillis();

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

                        // NOTE: Do NOT schedule alarm here! MainActivity will schedule after database insert completes.
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    updateReminderTimeText();
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private void updateReminderTimeText() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
            if (binding.textReminderTime != null) {
                binding.textReminderTime.setText(sdf.format(calendar.getTime()));
            }
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
