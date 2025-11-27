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
        int accentColor = prefs.getInt("accent_color", 0xFF34C759);

        binding.fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));

        if (adapter != null) {
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
        setupBlurEffect();

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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return false;
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

                    private final Paint circlePaint = new Paint();
                    private final Paint backgroundPaint = new Paint();

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
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
                                            taskViewModel.softDelete(task);
                                            viewHolder.itemView.setAlpha(1f);
                                            viewHolder.itemView.setScaleX(1f);
                                            viewHolder.itemView.setScaleY(1f);
                                            viewHolder.itemView.setRotation(0f);
                                            viewHolder.itemView.setTranslationX(0f);
                                            Snackbar.make(binding.getRoot(), "Task moved to Recycle Bin",
                                                    Snackbar.LENGTH_LONG)
                                                    .setAction("Undo", v -> taskViewModel.restore(task))
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
                                        .withEndAction(() -> viewHolder.itemView.animate()
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .rotation(0f)
                                                .setDuration(150)
                                                .start())
                                        .start();

                                // Open edit screen after delay to ensure item is restored
                                new android.os.Handler(android.os.Looper.getMainLooper())
                                        .postDelayed(() -> onTaskClicked(taskToEdit), 100);
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
                            deleteIcon = ContextCompat.getDrawable(MainActivity.this,
                                    R.drawable.ic_swipe_delete_custom);
                        }
                        if (editIcon == null) {
                            editIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_swipe_edit);
                        }

                        // Much lower threshold for easier activation
                        float swipeThreshold = itemView.getWidth() * 0.15f;

                        if (dX < 0) { // Swiping LEFT (Delete) - HEAVY ANIMATIONS
                            float swipeProgress = Math.min(Math.abs(dX) / swipeThreshold, 1.0f);

                            // LIGHTER MATTE RED (40% transparent = 60% opacity = 99 hex)
                            int startColor = Color.parseColor("#99E57373"); // Light Matte Red
                            int endColor = Color.parseColor("#99EF5350"); // Slightly darker
                            int currentColor = interpolateColor(startColor, endColor, swipeProgress);

                            backgroundPaint.setColor(currentColor);
                            android.graphics.RectF backgroundRect = new android.graphics.RectF(
                                    itemView.getRight() + (int) dX, itemView.getTop(),
                                    itemView.getRight(), itemView.getBottom());
                            c.drawRoundRect(backgroundRect, 30f, 30f, backgroundPaint);

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
                            // ELEVATION EFFECT
                            itemView.setElevation(20f * swipeProgress);

                        } else if (dX > 0) { // Swiping RIGHT (Edit) - HEAVY ANIMATIONS
                            float swipeProgress = Math.min(dX / swipeThreshold, 1.0f);

                            // LIGHTER MATTE GREEN (40% transparent = 60% opacity = 99 hex)
                            int startColor = Color.parseColor("#9981C784"); // Light Matte Green
                            int endColor = Color.parseColor("#9966BB6A"); // Slightly darker
                            int currentColor = interpolateColor(startColor, endColor, swipeProgress);

                            backgroundPaint.setColor(currentColor);
                            android.graphics.RectF backgroundRect = new android.graphics.RectF(itemView.getLeft(),
                                    itemView.getTop(),
                                    itemView.getLeft() + (int) dX, itemView.getBottom());
                            c.drawRoundRect(backgroundRect, 30f, 30f, backgroundPaint);

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
                            // ELEVATION EFFECT
                            itemView.setElevation(20f * swipeProgress);
                        } else {
                            // Reset all transformations
                            itemView.setScaleX(1.0f);
                            itemView.setScaleY(1.0f);
                            itemView.setRotation(0f);
                            itemView.setRotation(0f);
                            itemView.setElevation(0f);
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
                        viewHolder.itemView.setRotation(0f);
                        viewHolder.itemView.setElevation(0f);
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

    private void setupBlurEffect() {
        android.widget.ImageView blurOverlay = findViewById(R.id.blur_overlay);

        // Remove default scrim
        binding.drawerLayout.setScrimColor(android.graphics.Color.TRANSPARENT);

        binding.drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // Ensure blur is generated if we are sliding and it's not visible yet
                if (slideOffset > 0 && blurOverlay.getVisibility() != View.VISIBLE) {
                    generateBlur(blurOverlay);
                }

                if (blurOverlay.getVisibility() == View.VISIBLE) {
                    blurOverlay.setAlpha(slideOffset);
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                blurOverlay.setAlpha(1f);
                blurOverlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                blurOverlay.setVisibility(View.GONE);
                blurOverlay.setImageBitmap(null); // Free memory
                if (Build.VERSION.SDK_INT >= 31) {
                    blurOverlay.setRenderEffect(null);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // We handle generation in onDrawerSlide now for better coverage
            }
        });
    }

    private void generateBlur(android.widget.ImageView blurOverlay) {
        View content = binding.drawerLayout.getChildAt(0);
        if (content.getWidth() > 0 && content.getHeight() > 0) {
            try {
                android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                        content.getWidth(), content.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                content.draw(canvas);

                if (Build.VERSION.SDK_INT >= 31) {
                    blurOverlay.setImageBitmap(bitmap);
                    blurOverlay.setRenderEffect(RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.MIRROR));
                } else {
                    // Apply blur
                    android.graphics.Bitmap blurred = applyBlur(bitmap);
                    blurOverlay.setImageBitmap(blurred);
                }

                blurOverlay.setVisibility(View.VISIBLE);
                blurOverlay.setAlpha(0f);
            } catch (Exception e) {
                Log.e(TAG, "Error creating blur effect", e);
            }
        }
    }

    private android.graphics.Bitmap applyBlur(android.graphics.Bitmap image) {
        // Scale down for performance and "free" blur
        float scale = 0.2f; // 1/5th size
        int width = Math.round(image.getWidth() * scale);
        int height = Math.round(image.getHeight() * scale);

        if (width <= 0 || height <= 0)
            return image;

        android.graphics.Bitmap inputBitmap = android.graphics.Bitmap.createScaledBitmap(image, width, height, false);
        android.graphics.Bitmap outputBitmap = android.graphics.Bitmap.createBitmap(inputBitmap);

        // Fast blur algorithm (StackBlur variant simplified)
        return fastBlur(outputBitmap); // Increased radius for stronger blur
    }

    // Fast blur algorithm
    private android.graphics.Bitmap fastBlur(android.graphics.Bitmap sentBitmap) {
        final int radius = 20;
        android.graphics.Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }
}
