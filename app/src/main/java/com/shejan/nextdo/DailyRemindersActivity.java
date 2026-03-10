package com.shejan.nextdo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.shejan.nextdo.databinding.ActivityDailyRemindersBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyRemindersActivity extends AppCompatActivity implements DashboardTaskAdapter.OnTaskActionListener {

    private ActivityDailyRemindersBinding binding;
    private TaskViewModel taskViewModel;
    private DashboardTaskAdapter undoneAdapter;
    private DashboardTaskAdapter doneAdapter;
    private long selectedTimestamp;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private final ActivityResultLauncher<Intent> taskActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Refresh is handled by LiveData observation
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDailyRemindersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        selectedTimestamp = getIntent().getLongExtra("SELECTED_TIMESTAMP", System.currentTimeMillis());

        setupToolbar();
        setupRecyclerViews();
        setupViewModel();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String dateStr = dateFormat.format(new Date(selectedTimestamp));
        binding.textToolbarTitle.setText(dateStr);
    }

    private void setupRecyclerViews() {
        undoneAdapter = new DashboardTaskAdapter(this);
        undoneAdapter.setShowTimeline(false);
        binding.recyclerUndoneTasks.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerUndoneTasks.setAdapter(undoneAdapter);

        doneAdapter = new DashboardTaskAdapter(this);
        doneAdapter.setShowTimeline(false);
        binding.recyclerDoneTasks.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDoneTasks.setAdapter(doneAdapter);
    }

    private void setupViewModel() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Observe all non-deleted tasks (both active and completed) for this date
        taskViewModel.getAllNonDeletedTasks().observe(this, tasks -> {
            filterAndDisplayTasks(tasks);
        });
    }

    private void filterAndDisplayTasks(List<Task> allTasks) {
        if (allTasks == null)
            return;

        List<Task> undoneTasks = new ArrayList<>();
        List<Task> doneTasks = new ArrayList<>();

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(selectedTimestamp);

        for (Task task : allTasks) {
            if (task.reminderTime > 0) {
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTimeInMillis(task.reminderTime);

                if (isSameDay(targetCal, taskCal)) {
                    if (task.isCompleted) {
                        doneTasks.add(task);
                    } else {
                        undoneTasks.add(task);
                    }
                }
            }
        }

        undoneAdapter.setTasks(undoneTasks);
        doneAdapter.setTasks(doneTasks);

        // Visibility Toggles
        binding.textUndoneHeader.setVisibility(undoneTasks.isEmpty() ? View.GONE : View.VISIBLE);
        binding.recyclerUndoneTasks.setVisibility(undoneTasks.isEmpty() ? View.GONE : View.VISIBLE);

        binding.textDoneHeader.setVisibility(doneTasks.isEmpty() ? View.GONE : View.VISIBLE);
        binding.recyclerDoneTasks.setVisibility(doneTasks.isEmpty() ? View.GONE : View.VISIBLE);

        if (undoneTasks.isEmpty() && doneTasks.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        task.isCompleted = isChecked;
        taskViewModel.update(task);
    }

    @Override
    public void onTaskEdit(Task task) {
        Intent intent = new Intent(this, NewTaskActivity.class);
        intent.putExtra(NewTaskActivity.EXTRA_ID, task.id);
        intent.putExtra(NewTaskActivity.EXTRA_ALARM_ID, task.alarmId);
        intent.putExtra(NewTaskActivity.EXTRA_TITLE, task.title);
        intent.putExtra(NewTaskActivity.EXTRA_DESCRIPTION, task.description);
        intent.putExtra(NewTaskActivity.EXTRA_REMINDER_TIME, task.reminderTime);
        intent.putExtra(NewTaskActivity.EXTRA_REPEAT, task.repeat);
        intent.putExtra(NewTaskActivity.EXTRA_REMINDER_TYPE, task.reminderType);
        taskActivityLauncher.launch(intent);
    }

    @Override
    public void onTaskDelete(Task task) {
        taskViewModel.softDelete(task);
    }
}
