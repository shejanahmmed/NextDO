package com.shejan.nextdo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
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
    public static final int RESULT_DELETE = 2;

    private ActivityNewTaskBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private int taskId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityNewTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.priority_array));
        ((AutoCompleteTextView) binding.priorityAutoCompleteTextView).setAdapter(priorityAdapter);

        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.repeat_array));
        ((AutoCompleteTextView) binding.repeatAutoCompleteTextView).setAdapter(repeatAdapter);


        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Todo");
            }
            taskId = intent.getIntExtra(EXTRA_ID, -1);
            String title = intent.getStringExtra(EXTRA_TITLE);
            String description = intent.getStringExtra(EXTRA_DESCRIPTION);

            binding.editTitle.setText(title != null ? title : "");
            binding.editDescription.setText(description != null ? description : "");

            String priority = intent.getStringExtra(EXTRA_PRIORITY);
            if (priority != null) {
                ((AutoCompleteTextView) binding.priorityAutoCompleteTextView).setText(priority, false);
            }

            String repeat = intent.getStringExtra(EXTRA_REPEAT);
            if (repeat != null) {
                ((AutoCompleteTextView) binding.repeatAutoCompleteTextView).setText(repeat, false);
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

        binding.buttonSetReminder.setOnClickListener(v -> showDateTimePicker());

        binding.buttonSave.setOnClickListener(view -> {
            Intent replyIntent = new Intent();
            if (TextUtils.isEmpty(binding.editTitle.getText())) {
                setResult(RESULT_CANCELED, replyIntent);
            } else {
                String title = binding.editTitle.getText().toString();
                String description = binding.editDescription.getText().toString();
                String priority = ((AutoCompleteTextView) binding.priorityAutoCompleteTextView).getText().toString();
                String repeat = ((AutoCompleteTextView) binding.repeatAutoCompleteTextView).getText().toString();
                long reminderTime = calendar.getTimeInMillis();

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
            }
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (taskId != -1) {
            getMenuInflater().inflate(R.menu.menu_edit_task, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            Intent replyIntent = new Intent();
            replyIntent.putExtra(EXTRA_ID, taskId);
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
