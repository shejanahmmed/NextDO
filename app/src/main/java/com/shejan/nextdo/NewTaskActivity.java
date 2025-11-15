package com.shejan.nextdo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.shejan.nextdo.databinding.ActivityNewTaskBinding;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NewTaskActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "com.shejan.nextdo.ID";
    public static final String EXTRA_TITLE = "com.shejan.nextdo.TITLE";
    public static final String EXTRA_DESCRIPTION = "com.shejan.nextdo.DESCRIPTION";
    public static final String EXTRA_PRIORITY = "com.shejan.nextdo.PRIORITY";
    public static final String EXTRA_REMINDER_TIME = "com.shejan.nextdo.REMINDER_TIME";
    public static final String EXTRA_REPEAT = "com.shejan.nextdo.REPEAT";

    private ActivityNewTaskBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private int taskId = -1;
    private ArrayAdapter<CharSequence> priorityAdapter;
    private ArrayAdapter<CharSequence> repeatAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            ThemeManager.applyTheme(this);
        } catch (Exception e) {
            // Continue with default theme if theme application fails
        }

        binding = ActivityNewTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            priorityAdapter = ArrayAdapter.createFromResource(this,
                    R.array.priority_array, android.R.layout.simple_spinner_item);
            priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (binding.spinnerPriority != null) {
                binding.spinnerPriority.setAdapter(priorityAdapter);
            }

            repeatAdapter = ArrayAdapter.createFromResource(this,
                    R.array.repeat_array, android.R.layout.simple_spinner_item);
            repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (binding.spinnerRepeat != null) {
                binding.spinnerRepeat.setAdapter(repeatAdapter);
            }
        } catch (Exception e) {
            // Handle adapter creation failure
            finish();
            return;
        }

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            taskId = intent.getIntExtra(EXTRA_ID, -1);
            String title = intent.getStringExtra(EXTRA_TITLE);
            String description = intent.getStringExtra(EXTRA_DESCRIPTION);

            binding.editTitle.setText(title != null ? title : "");
            binding.editDescription.setText(description != null ? description : "");

            String priority = intent.getStringExtra(EXTRA_PRIORITY);
            if (priority != null && priorityAdapter != null) {
                int spinnerPosition = priorityAdapter.getPosition(priority);
                if (spinnerPosition >= 0) {
                    binding.spinnerPriority.setSelection(spinnerPosition);
                }
            }

            String repeat = intent.getStringExtra(EXTRA_REPEAT);
            if (repeat != null && repeatAdapter != null) {
                int spinnerPosition = repeatAdapter.getPosition(repeat);
                if (spinnerPosition >= 0) {
                    binding.spinnerRepeat.setSelection(spinnerPosition);
                }
            }

            long reminderTime = intent.getLongExtra(EXTRA_REMINDER_TIME, 0);
            if (reminderTime > 0) {
                calendar.setTimeInMillis(reminderTime);
                updateReminderTimeText();
            }
        }

        binding.buttonSetReminder.setOnClickListener(v -> showDateTimePicker());

        if (binding.buttonSave != null) {
            binding.buttonSave.setOnClickListener(view -> {
                Intent replyIntent = new Intent();
                if (binding.editTitle == null || TextUtils.isEmpty(binding.editTitle.getText())) {
                    setResult(RESULT_CANCELED, replyIntent);
                } else {
                    try {
                        String title = binding.editTitle.getText().toString();
                        String description = binding.editDescription != null ? binding.editDescription.getText().toString() : "";
                        String priority = binding.spinnerPriority != null && binding.spinnerPriority.getSelectedItem() != null ? 
                                binding.spinnerPriority.getSelectedItem().toString() : "NONE";
                        long reminderTime = calendar.getTimeInMillis();
                        String repeat = binding.spinnerRepeat != null && binding.spinnerRepeat.getSelectedItem() != null ? 
                                binding.spinnerRepeat.getSelectedItem().toString() : "NONE";

                        if (taskId != -1) {
                            replyIntent.putExtra(EXTRA_ID, taskId);
                        }
                        replyIntent.putExtra(EXTRA_TITLE, title);
                        replyIntent.putExtra(EXTRA_DESCRIPTION, description);
                        replyIntent.putExtra(EXTRA_PRIORITY, priority);
                        replyIntent.putExtra(EXTRA_REMINDER_TIME, reminderTime);
                        replyIntent.putExtra(EXTRA_REPEAT, repeat);

                        if (reminderTime > System.currentTimeMillis()) {
                            scheduleReminder(title, reminderTime, taskId != -1 ? taskId : (int) System.currentTimeMillis());
                        }

                        setResult(RESULT_OK, replyIntent);
                    } catch (Exception e) {
                        setResult(RESULT_CANCELED, replyIntent);
                    }
                }
                finish();
            });
        }
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
                    updateReminderTimeText();
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private void updateReminderTimeText() {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        binding.textReminderTime.setText(dateFormat.format(calendar.getTime()));
    }

    private void scheduleReminder(String taskTitle, long reminderTime, int taskId) {
        long delay = reminderTime - System.currentTimeMillis();

        Data data = new Data.Builder()
                .putString(ReminderWorker.TASK_TITLE, taskTitle)
                .putInt(ReminderWorker.TASK_ID, taskId)
                .build();

        OneTimeWorkRequest reminderWorkRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).enqueue(reminderWorkRequest);
    }
}
