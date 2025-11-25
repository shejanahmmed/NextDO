package com.shejan.nextdo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
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
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
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
                    task.priority = data.getStringExtra(NewTaskActivity.EXTRA_PRIORITY);
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
                        Toast.makeText(this, "Task can\'t be deleted", Toast.LENGTH_SHORT).show();
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
        int accentColor = prefs.getInt("accent_color", 0xFF34C759);

        if (binding.fab != null) {
            binding.fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        }

        if (adapter != null) {
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the ConstraintLayout inside DrawerLayout to avoid overlap
            // We find the ConstraintLayout (first child of DrawerLayout)
            View content = binding.drawerLayout.getChildAt(0);
            content.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize AlarmScheduler
        alarmScheduler = new AlarmScheduler(this);
        Log.d(TAG, "AlarmScheduler initialized");

        // Remove toolbar for Nothing theme
        setupNavigationDrawer();

        askNotificationPermission();

        TaskViewModelFactory factory = new TaskViewModelFactory(getApplication());
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        adapter = new TaskListAdapter(new TaskListAdapter.TaskDiff(), this);
        binding.recyclerview.setAdapter(adapter);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));

        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                adapter.submitList(tasks, () -> {
                    if (shouldScrollToTop) {
                        binding.recyclerview.smoothScrollToPosition(0);
                        shouldScrollToTop = false;
                    }
                });
                if (binding.emptyView != null && binding.recyclerview != null) {
                    if (tasks.isEmpty()) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.recyclerview.setVisibility(View.GONE);
                    } else {
                        binding.emptyView.setVisibility(View.GONE);
                        binding.recyclerview.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        if (binding.fab != null) {

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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return false;
                    }

                    @Override
                    public boolean isItemViewSwipeEnabled() {
                        return true;
                    }

                    @Override
                    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                        return 0.3f; // Lower threshold for easier swipe
                    }

                    @Override
                    public float getSwipeEscapeVelocity(float defaultValue) {
                        return defaultValue * 0.5f; // Easier to trigger swipe
                    }

                    private final Paint textPaint = new Paint();
                    private final Paint iconPaint = new Paint();
                    private final Paint circlePaint = new Paint();
                    private final ColorDrawable background = new ColorDrawable();

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Task task = adapter.getTaskAt(position);

                            // Vibration feedback
                            try {
                                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(
                                        android.content.Context.VIBRATOR_SERVICE);
                                if (vibrator != null) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(50,
                                                android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                                        vibrator.vibrate(effect);
                                    } else {
                                        // Deprecated in API 26
                                        vibrator.vibrate(50);
                                    }
                                }
                            } catch (Exception e) {
                                // Vibration not available
                            }

                            if (direction == ItemTouchHelper.LEFT) {
                                // HEAVY DELETE ANIMATION
                                viewHolder.itemView.animate()
                                        .alpha(0f)
                                        .scaleX(0.5f)
                                        .scaleY(0.5f)
                                        .rotation(15f)
                                        .translationX(-viewHolder.itemView.getWidth())
                                        .setDuration(400)
                                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                        .withEndAction(() -> {
                                            taskViewModel.delete(task);
                                            viewHolder.itemView.setAlpha(1f);
                                            viewHolder.itemView.setScaleX(1f);
                                            viewHolder.itemView.setScaleY(1f);
                                            viewHolder.itemView.setRotation(0f);
                                            viewHolder.itemView.setTranslationX(0f);
                                            Snackbar.make(binding.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                                                    .setAction("Undo", v -> taskViewModel.insert(task))
                                                    .show();
                                        })
                                        .start();
                            } else if (direction == ItemTouchHelper.RIGHT) {
                                // RIGHT SWIPE - Edit action
                                final Task taskToEdit = task;

                                // CRITICAL FIX: Reset the item immediately to prevent removal
                                adapter.notifyItemChanged(position);

                                // Play bounce animation
                                viewHolder.itemView.animate()
                                        .scaleX(1.15f)
                                        .scaleY(1.15f)
                                        .rotation(-3f)
                                        .setDuration(150)
                                        .setInterpolator(new android.view.animation.OvershootInterpolator())
                                        .withEndAction(() -> {
                                            viewHolder.itemView.animate()
                                                    .scaleX(1f)
                                                    .scaleY(1f)
                                                    .rotation(0f)
                                                    .setDuration(150)
                                                    .start();
                                        })
                                        .start();

                                // Open edit screen after delay to ensure item is restored
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    onTaskClicked(taskToEdit);
                                }, 100);
                            }
                        }
                    }

                    private Drawable deleteIcon;
                    private Drawable editIcon;

                    @Override
                    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
                            boolean isCurrentlyActive) {
                        View itemView = viewHolder.itemView;

                        // Initialize icons if needed
                        if (deleteIcon == null) {
                            deleteIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_swipe_delete);
                        }
                        if (editIcon == null) {
                            editIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_swipe_edit);
                        }

                        // Much lower threshold for easier activation
                        float swipeThreshold = itemView.getWidth() * 0.15f;

                        if (dX < 0) { // Swiping LEFT (Delete) - HEAVY ANIMATIONS
                            float swipeProgress = Math.min(Math.abs(dX) / swipeThreshold, 1.0f);

                            // LIGHTER MATTE RED
                            int startColor = Color.parseColor("#E57373"); // Light Matte Red
                            int endColor = Color.parseColor("#EF5350"); // Slightly darker
                            int currentColor = interpolateColor(startColor, endColor, swipeProgress);

                            background.setColor(currentColor);
                            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                                    itemView.getRight(), itemView.getBottom());
                            background.draw(c);

                            // PULSING CIRCLE EFFECT
                            circlePaint.setColor(Color.WHITE);
                            circlePaint.setAlpha((int) (100 * swipeProgress));
                            circlePaint.setAntiAlias(true);
                            float pulseRadius = 70f * swipeProgress
                                    * (1 + 0.3f * (float) Math.sin(System.currentTimeMillis() / 100.0));
                            float circleCenterX = itemView.getRight() - 120;
                            float circleCenterY = itemView.getTop() + (itemView.getHeight() / 2f);
                            c.drawCircle(circleCenterX, circleCenterY, pulseRadius, circlePaint);

                            // LARGE ANIMATED ICON (PNG)
                            if (deleteIcon != null) {
                                float iconSize = 70f * swipeProgress;
                                float iconCenterX = itemView.getRight() - 120;
                                float iconCenterY = itemView.getTop() + (itemView.getHeight() / 2f);

                                int halfSize = (int) (iconSize / 2);
                                deleteIcon.setBounds(
                                        (int) (iconCenterX - halfSize),
                                        (int) (iconCenterY - halfSize),
                                        (int) (iconCenterX + halfSize),
                                        (int) (iconCenterY + halfSize));
                                deleteIcon.setAlpha((int) (255 * swipeProgress));
                                deleteIcon.draw(c);
                            }

                            // LARGE BOLD TEXT
                            textPaint.setColor(Color.WHITE);
                            textPaint.setTextSize(42f * swipeProgress);
                            textPaint.setAntiAlias(true);
                            textPaint.setTextAlign(Paint.Align.CENTER);
                            textPaint.setAlpha((int) (255 * swipeProgress));
                            textPaint.setFakeBoldText(true);

                            String deleteText = "DELETE";
                            // Adjust text position relative to the icon size
                            float textY = itemView.getTop() + (itemView.getHeight() / 2f) + (70f * swipeProgress / 2)
                                    + 40;
                            c.drawText(deleteText, circleCenterX, textY, textPaint);

                            // DRAMATIC SCALE AND ROTATION
                            float scale = 1.0f - (swipeProgress * 0.15f);
                            float rotation = swipeProgress * 8f;
                            itemView.setScaleX(scale);
                            itemView.setScaleY(scale);
                            itemView.setRotation(rotation);

                            // ELEVATION EFFECT
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                itemView.setElevation(20f * swipeProgress);
                            }

                        } else if (dX > 0) { // Swiping RIGHT (Edit) - HEAVY ANIMATIONS
                            float swipeProgress = Math.min(dX / swipeThreshold, 1.0f);

                            // LIGHTER MATTE GREEN
                            int startColor = Color.parseColor("#81C784"); // Light Matte Green
                            int endColor = Color.parseColor("#66BB6A"); // Slightly darker
                            int currentColor = interpolateColor(startColor, endColor, swipeProgress);

                            background.setColor(currentColor);
                            background.setBounds(itemView.getLeft(), itemView.getTop(),
                                    itemView.getLeft() + (int) dX, itemView.getBottom());
                            background.draw(c);

                            // PULSING CIRCLE EFFECT
                            circlePaint.setColor(Color.WHITE);
                            circlePaint.setAlpha((int) (100 * swipeProgress));
                            circlePaint.setAntiAlias(true);
                            float pulseRadius = 70f * swipeProgress
                                    * (1 + 0.3f * (float) Math.sin(System.currentTimeMillis() / 100.0));
                            float circleCenterX = itemView.getLeft() + 120;
                            float circleCenterY = itemView.getTop() + (itemView.getHeight() / 2f);
                            c.drawCircle(circleCenterX, circleCenterY, pulseRadius, circlePaint);

                            // LARGE ANIMATED ICON (PNG)
                            if (editIcon != null) {
                                float iconSize = 70f * swipeProgress;
                                float iconCenterX = itemView.getLeft() + 120;
                                float iconCenterY = itemView.getTop() + (itemView.getHeight() / 2f);

                                int halfSize = (int) (iconSize / 2);
                                editIcon.setBounds(
                                        (int) (iconCenterX - halfSize),
                                        (int) (iconCenterY - halfSize),
                                        (int) (iconCenterX + halfSize),
                                        (int) (iconCenterY + halfSize));
                                editIcon.setAlpha((int) (255 * swipeProgress));
                                editIcon.draw(c);
                            }

                            // LARGE BOLD TEXT
                            textPaint.setColor(Color.WHITE);
                            textPaint.setTextSize(42f * swipeProgress);
                            textPaint.setAntiAlias(true);
                            textPaint.setTextAlign(Paint.Align.CENTER);
                            textPaint.setAlpha((int) (255 * swipeProgress));
                            textPaint.setFakeBoldText(true);

                            String editText = "EDIT";
                            // Adjust text position relative to the icon size
                            float textY = itemView.getTop() + (itemView.getHeight() / 2f) + (70f * swipeProgress / 2)
                                    + 40;
                            c.drawText(editText, circleCenterX, textY, textPaint);

                            // DRAMATIC SCALE AND ROTATION
                            float scale = 1.0f - (swipeProgress * 0.15f);
                            float rotation = -swipeProgress * 8f;
                            itemView.setScaleX(scale);
                            itemView.setScaleY(scale);
                            itemView.setRotation(rotation);

                            // ELEVATION EFFECT
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                itemView.setElevation(20f * swipeProgress);
                            }
                        } else {
                            // Reset all transformations
                            itemView.setScaleX(1.0f);
                            itemView.setScaleY(1.0f);
                            itemView.setRotation(0f);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                itemView.setElevation(0f);
                            }
                        }

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);
                        // Reset all transformations when swipe is cancelled
                        viewHolder.itemView.setScaleX(1.0f);
                        viewHolder.itemView.setScaleY(1.0f);
                        viewHolder.itemView.setRotation(0f);
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            viewHolder.itemView.setElevation(0f);
                        }
                    }

                    // Helper method for smooth color interpolation
                    private int interpolateColor(int startColor, int endColor, float fraction) {
                        int startA = Color.alpha(startColor);
                        int startR = Color.red(startColor);
                        int startG = Color.green(startColor);
                        int startB = Color.blue(startColor);

                        int endA = Color.alpha(endColor);
                        int endR = Color.red(endColor);
                        int endG = Color.green(endColor);
                        int endB = Color.blue(endColor);

                        return Color.argb(
                                (int) (startA + fraction * (endA - startA)),
                                (int) (startR + fraction * (endR - startR)),
                                (int) (startG + fraction * (endG - startG)),
                                (int) (startB + fraction * (endB - startB)));
                    }
                });
        itemTouchHelper.attachToRecyclerView(binding.recyclerview);
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

        int drawableId = 0;
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

    private void setupNavigationDrawer() {
        binding.menuIcon.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.END));

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END);
            return true;
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
        intent.putExtra(NewTaskActivity.EXTRA_PRIORITY, task.priority);
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
        dialog.getWindow()
                .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

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
