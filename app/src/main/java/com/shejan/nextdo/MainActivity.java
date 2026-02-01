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
        applyBackground();

        // Refresh accent color
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);

        // Check for expired completed tasks (15 days)
        long fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000;
        long threshold = System.currentTimeMillis() - fifteenDaysInMillis;
        taskViewModel.deleteOldCompletedTasks(threshold);
        int accentColor = prefs.getInt("accent_color", 0xFF34C759);

        binding.fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));

        if (adapter != null) {
            adapter.setAccentColor(accentColor);
            // noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Edge-to-Edge
        binding.drawerLayout.setDrawerElevation(0f);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the main content container only
            View content = findViewById(R.id.main_content_container);
            if (content != null) {
                content.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize AlarmScheduler
        alarmScheduler = new AlarmScheduler(this);
        Log.d(TAG, "AlarmScheduler initialized");

        // Remove toolbar for Nothing theme
        // Remove toolbar for Nothing theme
        setupDrawer();
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

        // Task Swipe Swipe Callback
        setupSwipeGestures();
    }

    private void setupSwipeGestures() {
        TaskSwipeCallback swipeCallback = new TaskSwipeCallback(this, adapter,
                new TaskSwipeCallback.SwipeActionReceiver() {
                    @Override
                    public void onSwipedLeft(Task task, RecyclerView.ViewHolder viewHolder) {
                        // DELETE ANIMATION
                        viewHolder.itemView.animate()
                                .alpha(0f)
                                .scaleX(0.5f)
                                .scaleY(0.5f)
                                .rotation(15f)
                                .translationX(-viewHolder.itemView.getWidth())
                                .setDuration(400)
                                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                .withEndAction(() -> {
                                    taskViewModel.softDelete(task);
                                    // Reset view state
                                    viewHolder.itemView.setAlpha(1f);
                                    viewHolder.itemView.setScaleX(1f);
                                    viewHolder.itemView.setScaleY(1f);
                                    viewHolder.itemView.setRotation(0f);
                                    viewHolder.itemView.setTranslationX(0f);

                                    Snackbar.make(binding.getRoot(), "Task moved to Recycle Bin", Snackbar.LENGTH_LONG)
                                            .setAction("Undo", v -> taskViewModel.restore(task))
                                            .show();
                                })
                                .start();
                    }

                    @Override
                    public void onSwipedRight(Task task, int position) {
                        // Play bounce animation
                        RecyclerView.ViewHolder viewHolder = binding.recyclerview
                                .findViewHolderForAdapterPosition(position);
                        if (viewHolder != null) {
                            viewHolder.itemView.animate()
                                    .scaleX(1.15f)
                                    .scaleY(1.15f)
                                    .rotation(-3f)
                                    .setDuration(150)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                                    .withEndAction(() -> viewHolder.itemView.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .rotation(0f)
                                            .setDuration(150)
                                            .start())
                                    .start();
                        }

                        // Open edit screen after delay
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .postDelayed(() -> onTaskClicked(task), 100);
                    }
                });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerview);
    }

    private void setupDrawer() {
        binding.menuIcon.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.END));

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_about) {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_releases) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/shejanahmmed/NextDO/releases"));
                    startActivity(intent);
                } catch (Exception e) {
                    // Handle potential errors
                }
            } else if (id == R.id.nav_help) {
                Intent intent = new Intent(MainActivity.this, HelpFAQActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_completed_tasks) {
                Intent intent = new Intent(MainActivity.this, CompletedTasksActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_recycle_bin) {
                Intent intent = new Intent(MainActivity.this, RecycleBinActivity.class);
                startActivity(intent);
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // Setup Close Button in Header
        android.view.View headerView = binding.navView.getHeaderView(0);
        android.view.View closeButton = headerView.findViewById(R.id.close_drawer_button);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> binding.drawerLayout.closeDrawer(GravityCompat.END));
        }
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

    private void applyBackground() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);
        String background = prefs.getString("app_background", "default");

        // Find the ConstraintLayout inside the DrawerLayout
        View content = binding.drawerLayout.getChildAt(0);

        if ("custom".equals(background)) {
            try {
                java.io.File file = new java.io.File(getFilesDir(), "custom_background.jpg");
                if (file.exists()) {
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                    android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(
                            getResources(), bitmap);
                    content.setBackground(drawable);
                    return;
                }
            } catch (Exception e) {
                // Fallback to default if loading fails
            }
        }

        int drawableId;
        switch (background) {
            case "bg_night_cottage":
                drawableId = R.drawable.bg_night_cottage;
                break;
            case "bg_urban_sketch":
                drawableId = R.drawable.bg_urban_sketch;
                break;
            case "bg_mystic_tree":
                drawableId = R.drawable.bg_mystic_tree;
                break;
            case "bg_dark_waves":
                drawableId = R.drawable.bg_dark_waves;
                break;
            default:
                drawableId = 0;
                break;
        }

        if (drawableId != 0) {
            content.setBackground(ContextCompat.getDrawable(this, drawableId));
        } else {
            // Default background (theme attribute)
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
            if (typedValue.resourceId != 0) {
                content.setBackgroundResource(typedValue.resourceId);
            } else {
                content.setBackgroundColor(typedValue.data);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        taskActivityLauncher.launch(intent);
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

}
