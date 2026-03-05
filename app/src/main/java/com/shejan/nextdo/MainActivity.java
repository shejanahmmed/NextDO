package com.shejan.nextdo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.shejan.nextdo.databinding.ActivityMainBinding;
import androidx.core.view.GravityCompat;

import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements TaskListAdapter.OnTaskInteractionListener {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private TaskViewModel taskViewModel;
    private AlarmScheduler alarmScheduler;
    private boolean shouldScrollToTop = false;
    private TaskListAdapter adapter;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notifications will not be shown.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> taskActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int id = data.getIntExtra(NewTaskActivity.EXTRA_ID, 0);
                    long reminderTime = data.getLongExtra(NewTaskActivity.EXTRA_REMINDER_TIME, 0);

                    Task task = new Task();
                    if (id != 0) { // Existing task - use the existing alarmId
                        task.id = id;
                        int existingAlarmId = data.getIntExtra(NewTaskActivity.EXTRA_ALARM_ID, 0);
                        task.alarmId = existingAlarmId;
                        Log.d(TAG, "Updating existing task " + id + " with alarmId=" + existingAlarmId);
                    } else { // New task - generate alarmId
                        task.alarmId = (int) System.currentTimeMillis();
                        Log.d(TAG, "New task: generated alarmId=" + task.alarmId);
                    }
                    task.title = data.getStringExtra(NewTaskActivity.EXTRA_TITLE);
                    task.description = data.getStringExtra(NewTaskActivity.EXTRA_DESCRIPTION);

                    task.reminderTime = reminderTime;
                    task.repeat = data.getStringExtra(NewTaskActivity.EXTRA_REPEAT);
                    task.reminderType = data.getStringExtra(NewTaskActivity.EXTRA_REMINDER_TYPE); // CRITICAL FIX!

                    Log.d(TAG, "MainActivity: Task reminderType = " + task.reminderType);

                    if (id != 0) {
                        Log.d(TAG, "Updating task " + id + " with reminderTime=" + reminderTime);

                        // Schedule alarm callback AFTER database update completes
                        final Task taskForCallback = task;
                        final long finalReminderTime = reminderTime;
                        taskViewModel.update(task, () -> {
                            Log.d(TAG, "Database update complete, scheduling alarm if needed");
                            if (finalReminderTime > 0 && taskForCallback.alarmId != 0) {
                                Log.d(TAG, "Scheduling alarm for updated task");
                                alarmScheduler.schedule(taskForCallback);
                            } else {
                                Log.d(TAG, "No reminder for updated task, canceling any existing alarm");
                                alarmScheduler.cancel(taskForCallback);
                            }
                        });
                    } else {
                        Log.d(TAG, "Inserting new task with reminderTime=" + reminderTime);

                        // Schedule alarm callback AFTER database insert completes
                        final Task taskForCallback = task;
                        final long finalReminderTime = reminderTime;
                        taskViewModel.insert(task, () -> {
                            Log.d(TAG, "Database insert complete, scheduling alarm if needed");
                            if (finalReminderTime > 0 && taskForCallback.alarmId != 0) {
                                Log.d(TAG, "Scheduling alarm for new task after database insert");
                                alarmScheduler.schedule(taskForCallback);
                            } else {
                                Log.d(TAG, "No reminder for new task");
                            }
                        });
                        shouldScrollToTop = true;
                    }
                } else if (result.getResultCode() == NewTaskActivity.RESULT_DELETE && result.getData() != null) {
                    Intent data = result.getData();
                    int id = data.getIntExtra(NewTaskActivity.EXTRA_ID, -1);
                    if (id == -1) {
                        Toast.makeText(this, "Task can't be deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Task task = new Task();
                    task.id = id;
                    taskViewModel.delete(task);
                }
            });

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh accent color
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);

        // Check for expired completed tasks (15 days)
        long fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000;
        long threshold = System.currentTimeMillis() - fifteenDaysInMillis;
        taskViewModel.deleteOldCompletedTasks(threshold);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyThemePreference();

        // ThemeManager.applyTheme(this); // REMOVED: Conflicting with
        // applyThemePreference

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize AlarmScheduler
        alarmScheduler = new AlarmScheduler(this);
        Log.d(TAG, "AlarmScheduler initialized");

        // Remove toolbar for Nothing theme
        // setupBlurEffect() removed;

        askNotificationPermission();

        TaskViewModelFactory factory = new TaskViewModelFactory(getApplication());
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        adapter = new TaskListAdapter(new TaskListAdapter.TaskDiff(), this);
        binding.recyclerview.setAdapter(adapter);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));

        taskViewModel.getActiveTasks().observe(this, tasks -> {
            if (tasks != null) {
                adapter.submitList(tasks, () -> {
                    // Force refresh because colors depend on position, which DiffUtil might not
                    // update
                    // if content is same but position changed.
                    // Wrap in post to ensure it runs after any internal processing
                    binding.recyclerview.post(() -> adapter.notifyDataSetChanged());

                    if (shouldScrollToTop) {
                        binding.recyclerview.smoothScrollToPosition(0);
                        shouldScrollToTop = false;
                    }
                });
                if (tasks.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.recyclerview.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.recyclerview.setVisibility(View.VISIBLE);
                }
            }
        });

        // Start floating animation
        android.view.animation.Animation floatAnimation = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.fab_float_animation);
        binding.fab.startAnimation(floatAnimation);

        binding.fab.setOnClickListener(view -> {
            android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this,
                    R.anim.fab_click_animation);
            binding.fab.startAnimation(animation);
            Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
            taskActivityLauncher.launch(intent);
        });

    }

    private void askNotificationPermission() {
        // This is only necessary for API level 33 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Launch the permission request
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onTaskCompleted(Task task, boolean isCompleted) {
        task.isCompleted = isCompleted;
        taskViewModel.update(task);

        // Always cancel notification when task is marked as completed
        if (isCompleted) {
            try {
                androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat
                        .from(this);
                notificationManager.cancel(task.id);
            } catch (Exception e) {
                // Handle cancellation errors silently
            }
        }
    }

    @Override
    public void onTaskClicked(Task task) {
        Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
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
        Snackbar.make(binding.getRoot(), "Task moved to Recycle Bin", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> taskViewModel.restore(task))
                .show();
    }

    @Override
    public void onTaskLongClicked(Task task) {
        showTaskContextMenu(task);
    }

    private void showTaskContextMenu(Task task) {
        if (task == null)
            return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_task_options, null);
        builder.setView(customView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        customView.findViewById(R.id.edit_option).setOnClickListener(v -> {
            dialog.dismiss();
            onTaskClicked(task);
        });

        customView.findViewById(R.id.delete_option).setOnClickListener(v -> {
            dialog.dismiss();
            taskViewModel.delete(task);
            Snackbar.make(binding.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo", view -> taskViewModel.insert(task))
                    .show();
        });

        dialog.show();
    }

    private void applyThemePreference() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);
        String theme = prefs.getString("app_theme", "light");

        int nightMode;
        switch (theme) {
            case "light":
                nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "auto":
            default:
                nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        // CRITICAL FIX: Only apply if different to avoid infinite recreation loops
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != nightMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

}
