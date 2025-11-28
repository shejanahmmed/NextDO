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

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets systemBars = windowInsets
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Apply top inset to toolbar (padding)
            binding.toolbar.setPadding(
                    binding.toolbar.getPaddingLeft(),
                    systemBars.top,
                    binding.toolbar.getPaddingRight(),
                    binding.toolbar.getPaddingBottom());

            // Apply bottom inset to RecyclerView (padding)
            binding.recyclerviewCompletedTasks.setPadding(
                    binding.recyclerviewCompletedTasks.getPaddingLeft(),
                    binding.recyclerviewCompletedTasks.getPaddingTop(),
                    binding.recyclerviewCompletedTasks.getPaddingRight(),
                    16 + systemBars.bottom // Original 16dp + inset
            );

            return windowInsets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

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
