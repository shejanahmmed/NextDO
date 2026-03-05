package com.shejan.nextdo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.shejan.nextdo.databinding.ActivityCompletedTasksBinding;

public class CompletedTasksActivity extends AppCompatActivity {

    private ActivityCompletedTasksBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        binding = ActivityCompletedTasksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar removed, using custom scrolling title. System back handles
        // navigation.

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets systemBars = windowInsets
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Apply padding to the main content container only (matching My Reminders page)
            android.view.View content = binding.mainContentContainer;
            if (content != null) {
                content.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }

            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        // Initialize ViewModel
        TaskViewModelFactory factory = new TaskViewModelFactory(getApplication());
        TaskViewModel taskViewModel = new androidx.lifecycle.ViewModelProvider(this, factory).get(TaskViewModel.class);

        // Setup RecyclerView
        TaskListAdapter adapter = new TaskListAdapter(new TaskListAdapter.TaskDiff(),
                new TaskListAdapter.OnTaskInteractionListener() {
                    @Override
                    public void onTaskCompleted(Task task, boolean isCompleted) {
                        task.isCompleted = isCompleted;
                        taskViewModel.update(task);
                    }

                    @Override
                    public void onTaskClicked(Task task) {
                        // Optional: Allow editing completed tasks?
                    }

                    @Override
                    public void onTaskLongClicked(Task task) {
                        // Optional: Show options
                    }

                    @Override
                    public void onTaskDelete(Task task) {
                        // Optional: Implement delete for completed tasks if needed
                        taskViewModel.delete(task);
                        com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
                                "Task deleted permanently", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                                .show();
                    }
                });
        binding.recyclerviewCompletedTasks.setAdapter(adapter);
        binding.recyclerviewCompletedTasks.setLayoutManager(new LinearLayoutManager(this));

        // Observe completed tasks
        taskViewModel.getCompletedTasks().observe(this, tasks -> {
            if (tasks != null) {
                adapter.submitList(tasks);
                if (tasks.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.recyclerviewCompletedTasks.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.recyclerviewCompletedTasks.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
