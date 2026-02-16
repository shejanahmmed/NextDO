package com.shejan.nextdo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements DashboardTaskAdapter.OnTaskCompletionListener {

    private RecyclerView recyclerDates;
    private RecyclerView recyclerTasks;
    private DashboardDateAdapter dateAdapter;
    private DashboardTaskAdapter taskAdapter;
    private ImageView btnMenu;
    private FloatingActionButton fab;
    private ProgressBar progressCircle;
    private ProgressBar progressCircleSecondary;
    private TextView textSummary;
    private TextView textProgressPercent;

    private TaskViewModel taskViewModel;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Added for saving tasks
    private AlarmScheduler alarmScheduler;
    private static final String TAG = "DashboardActivity";

    private final ActivityResultLauncher<Intent> taskActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int id = data.getIntExtra(NewTaskActivity.EXTRA_ID, 0);
                    long reminderTime = data.getLongExtra(NewTaskActivity.EXTRA_REMINDER_TIME, 0);

                    Task task = new Task();
                    if (id != 0) {
                        task.id = id;
                        int existingAlarmId = data.getIntExtra(NewTaskActivity.EXTRA_ALARM_ID, 0);
                        task.alarmId = existingAlarmId;
                    } else {
                        task.alarmId = (int) System.currentTimeMillis();
                    }
                    task.title = data.getStringExtra(NewTaskActivity.EXTRA_TITLE);
                    task.description = data.getStringExtra(NewTaskActivity.EXTRA_DESCRIPTION);
                    task.reminderTime = reminderTime;
                    task.repeat = data.getStringExtra(NewTaskActivity.EXTRA_REPEAT);
                    task.reminderType = data.getStringExtra(NewTaskActivity.EXTRA_REMINDER_TYPE);

                    if (id != 0) {
                        // Update existing (Future proofing, though FAB is usually for new)
                        final Task taskForCallback = task;
                        final long finalReminderTime = reminderTime;
                        taskViewModel.update(task, () -> {
                            if (finalReminderTime > 0 && taskForCallback.alarmId != 0) {
                                alarmScheduler.schedule(taskForCallback);
                            } else {
                                alarmScheduler.cancel(taskForCallback);
                            }
                        });
                    } else {
                        // Insert New
                        final Task taskForCallback = task;
                        final long finalReminderTime = reminderTime;
                        taskViewModel.insert(task, () -> {
                            if (finalReminderTime > 0 && taskForCallback.alarmId != 0) {
                                alarmScheduler.schedule(taskForCallback);
                            }
                        });
                    }
                }
            });

    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Task nextUpcomingTask = null;
    private Runnable countdownRunnable;

    private Calendar selectedDate = Calendar.getInstance();
    private List<Task> currentActiveTasks = new ArrayList<>();
    private List<Task> currentCompletedTasks = new ArrayList<>();
    private List<Task> dashboardShownTasks = new ArrayList<>(); // Track tasks shown in adapter
    private int currentlySwipedPosition = -1; // Track swipe by position for stability

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme preference immediately on launch
        applyThemePreference();

        // Hide default Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_dashboard);

        // Initialize Views
        recyclerDates = findViewById(R.id.recycler_dates);
        recyclerTasks = findViewById(R.id.recycler_tasks);
        btnMenu = findViewById(R.id.btn_menu);
        fab = findViewById(R.id.fab_dashboard);
        progressCircle = findViewById(R.id.progress_bar_circle);
        progressCircleSecondary = findViewById(R.id.progress_bar_circle_secondary);
        textSummary = findViewById(R.id.text_summary_line1);
        textProgressPercent = findViewById(R.id.text_progress_percent);

        TextView textMonthYear = findViewById(R.id.text_month_year);
        TextView textViewAll = findViewById(R.id.text_view_all);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup Header / Drawer
        setupDrawer();

        // Setup Date Scroller (Horizontal)
        recyclerDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dateAdapter = new DashboardDateAdapter();
        recyclerDates.setAdapter(dateAdapter);

        // Setup Timeline Tasks (Vertical)
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new DashboardTaskAdapter(this);
        recyclerTasks.setAdapter(taskAdapter);

        // Define Start and End Dates (Today - 15 to Today + 15)
        List<DashboardDateAdapter.DateItem> dateItems = generateDates();
        dateAdapter.setDates(dateItems);
        // Scroll to Today (Index 15 + 1 for Arrow = 16)
        recyclerDates.scrollToPosition(16);

        // Initialize Month/Year Text
        java.text.SimpleDateFormat monthYearFormat = new java.text.SimpleDateFormat("MMMM yyyy",
                java.util.Locale.getDefault());
        textMonthYear.setText(monthYearFormat.format(selectedDate.getTime()));

        // Handle Date Clicks
        dateAdapter.setOnDateClickListener((item, position) -> {
            // Update Selected Date
            selectedDate.setTimeInMillis(item.timestamp);

            // Update Month/Year Header
            textMonthYear.setText(monthYearFormat.format(selectedDate.getTime()));

            // Refresh Filtered List
            filterAndShowTasks();
        });

        textViewAll.setOnClickListener(v -> {
            // Navigate to Main Task List (View All)
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Initialize AlarmScheduler
        alarmScheduler = new AlarmScheduler(this);

        // Setup FAB
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, NewTaskActivity.class);
            taskActivityLauncher.launch(intent);
        });

        // Ensure Status Bar is consistent with design
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // Ensure light status bar icons if background is light
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Apply Window Insets to DrawerLayout (Root) to handle status bar height and
        // prevent default DrawerLayout scrim behavior
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Apply padding to the main content container only
            View content = findViewById(R.id.dashboard_content_container);
            if (content != null) {
                content.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }

            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        // Initialize ViewModel
        TaskViewModelFactory factory = new TaskViewModelFactory(getApplication());
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        // Observe Tasks
        taskViewModel.getActiveTasks().observe(this, activeTasks -> {
            currentActiveTasks = (activeTasks != null) ? activeTasks : new ArrayList<>();
            updateDashboard(currentActiveTasks, currentCompletedTasks);
        });

        taskViewModel.getCompletedTasks().observe(this, completedTasks -> {
            currentCompletedTasks = (completedTasks != null) ? completedTasks : new ArrayList<>();
            updateDashboard(currentActiveTasks, currentCompletedTasks);
        });

        taskViewModel.getCompletedTasks().observe(this, completedTasks -> {
            currentCompletedTasks = (completedTasks != null) ? completedTasks : new ArrayList<>();
            updateDashboard(currentActiveTasks, currentCompletedTasks);
        });
    }

    private void setupDrawer() {
        if (drawerLayout != null) {
            drawerLayout.setDrawerElevation(0f);
        }

        // Open Drawer on Menu Click
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // Remove the semi-transparent status bar overlay (scrim)
        if (drawerLayout != null) {
            drawerLayout.setStatusBarBackground(null);
        }

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) {
                    // Already on Dashboard
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (id == R.id.nav_tasks) {
                    // "My Tasks" in Drawer -> Launch Main Task List
                    Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_settings) {
                    Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_about) {
                    Intent intent = new Intent(DashboardActivity.this, AboutActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_releases) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/shejanahmmed/NextDO/releases"));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (id == R.id.nav_license) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/shejanahmmed/NextDO/blob/main/LICENSE"));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (id == R.id.nav_help) {
                    Intent intent = new Intent(DashboardActivity.this, HelpFAQActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_completed_tasks) {
                    Intent intent = new Intent(DashboardActivity.this, CompletedTasksActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_recycle_bin) {
                    Intent intent = new Intent(DashboardActivity.this, RecycleBinActivity.class);
                    startActivity(intent);
                }

                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            });

            // Set Dashboard as Checked
            navigationView.setCheckedItem(R.id.nav_dashboard);

            // Apply active styling
            applyActiveStateToMenuItem(R.id.nav_dashboard);

            // Setup footer close button
            View headerView = navigationView.getHeaderView(0);
            // Footer is not in header, it's in the LinearLayout container.
            // We need to find it from the activity root if accessible, or just let standard
            // behavior work.
            // In activity_dashboard.xml, the include has id drawer_footer_include.
            // Inside that include (drawer_footer.xml), there is likely a close button.
            // Let's try to find it.
            View btnClose = findViewById(R.id.btn_close_drawer_footer);
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
            }
        }
    }

    private void applyActiveStateToMenuItem(int menuItemId) {
        if (navigationView == null)
            return;
        android.view.Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == menuItemId) {
                item.setChecked(true);
                // Apply lavender background to checked item
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(getResources().getColor(R.color.drawer_lavender, null));
                bg.setCornerRadius(24 * getResources().getDisplayMetrics().density);
                // Note: NavigationView handles checked state styling automatically if
                // configured in XML style
            }
        }
    }

    private void updateDashboard(List<Task> activeTasks, List<Task> completedTasks) {
        if (activeTasks == null)
            activeTasks = new ArrayList<>();
        if (completedTasks == null)
            completedTasks = new ArrayList<>();

        // 1. Calculate Daily Goal Progress (Main Circle)
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        int todayTotal = 0;
        int todayCompleted = 0;

        // Filter Active Tasks for Today
        List<Task> todayActiveList = new ArrayList<>();
        for (Task task : activeTasks) {
            calendar.setTimeInMillis(task.reminderTime);
            if (task.reminderTime > 0 && calendar.get(Calendar.YEAR) == year
                    && calendar.get(Calendar.DAY_OF_YEAR) == dayOfYear) {
                todayTotal++;
                todayActiveList.add(task);
            }
        }

        // Filter Completed Tasks for Today
        for (Task task : completedTasks) {
            calendar.setTimeInMillis(task.reminderTime);
            if (task.reminderTime > 0 && calendar.get(Calendar.YEAR) == year
                    && calendar.get(Calendar.DAY_OF_YEAR) == dayOfYear) {
                todayTotal++;
                todayCompleted++;
            }
        }

        // Update Main Progress
        int mainProgress = (todayTotal > 0) ? (int) (((float) todayCompleted / todayTotal) * 100) : 0;
        progressCircle.setProgress(mainProgress);
        textProgressPercent.setText(mainProgress + "%");

        int todayLeft = todayTotal - todayCompleted;
        if (textSummary != null) {
            String summaryText = todayCompleted + " Done, <font color='#FF9AA2'><b>" + todayLeft + " Left</b></font>";
            textSummary.setText(Html.fromHtml(summaryText, Html.FROM_HTML_MODE_LEGACY));
        }

        // 2. Identify Next Upcoming Task (Secondary Circle)
        long now = System.currentTimeMillis();
        nextUpcomingTask = null;
        long minDiff = Long.MAX_VALUE;

        for (Task task : activeTasks) {
            if (task.reminderTime > now) {
                long diff = task.reminderTime - now;
                if (diff < minDiff) {
                    minDiff = diff;
                    nextUpcomingTask = task;
                }
            }
        }

        // Start Countdown for Secondary Circle
        startCountdown();

        // After updating dashboard, filter tasks for the currently selected date
        filterAndShowTasks();
    }

    private List<DashboardDateAdapter.DateItem> generateDates() {
        List<DashboardDateAdapter.DateItem> items = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Normalize selected date to Start of Day for accurate comparison if needed,
        // but for generation we act relative to "Today"

        // Go back 15 days
        cal.add(Calendar.DAY_OF_YEAR, -15);

        // Generate 31 days (15 prev + 1 today + 15 next)
        java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault());
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd", java.util.Locale.getDefault());

        for (int i = 0; i < 31; i++) {
            boolean isToday = (i == 15); // Index 15 is Today
            boolean isActive = (i == 15); // Index 15 is initially Active
            String day = dayFormat.format(cal.getTime()).toUpperCase();
            String date = dateFormat.format(cal.getTime());

            items.add(new DashboardDateAdapter.DateItem(day, date, cal.getTimeInMillis(), isActive, isToday));

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return items;
    }

    private void filterAndShowTasks() {
        if (currentActiveTasks == null)
            return;

        List<Task> filteredTasks = new ArrayList<>();
        Calendar taskCal = Calendar.getInstance();
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(selectedDate.getTimeInMillis());

        int selYear = selectedCal.get(Calendar.YEAR);
        int selDay = selectedCal.get(Calendar.DAY_OF_YEAR);

        List<Task> combinedTasks = new ArrayList<>();
        if (currentActiveTasks != null)
            combinedTasks.addAll(currentActiveTasks);
        if (currentCompletedTasks != null)
            combinedTasks.addAll(currentCompletedTasks);

        for (Task task : combinedTasks) {
            if (task.reminderTime > 0) {
                taskCal.setTimeInMillis(task.reminderTime);
                if (taskCal.get(Calendar.YEAR) == selYear &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == selDay) {
                    filteredTasks.add(task);
                }
            }
        }

        // update UI based on count
        TextView textEmptyState = findViewById(R.id.text_empty_state);
        if (filteredTasks.isEmpty()) {
            recyclerTasks.setVisibility(View.GONE);
            if (textEmptyState != null)
                textEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerTasks.setVisibility(View.VISIBLE);
            if (textEmptyState != null)
                textEmptyState.setVisibility(View.GONE);
        }

        // Sort by time (ASC) - Upcoming soon at top
        java.util.Collections.sort(filteredTasks, (t1, t2) -> Long.compare(t1.reminderTime, t2.reminderTime));

        this.dashboardShownTasks = filteredTasks;
        taskAdapter.setTasks(filteredTasks);
    }

    private void startCountdown() {
        countdownHandler.removeCallbacksAndMessages(null);

        if (nextUpcomingTask == null) {
            if (progressCircleSecondary != null)
                progressCircleSecondary.setProgress(0);
            return;
        }

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (nextUpcomingTask == null)
                    return;

                long now = System.currentTimeMillis();
                long timeLeft = nextUpcomingTask.reminderTime - now;

                if (timeLeft <= 0) {
                    if (progressCircleSecondary != null)
                        progressCircleSecondary.setProgress(0);
                    return;
                }

                // Scale: 100% = 60 Minutes (3600000 ms)
                long maxScale = 60 * 60 * 1000;

                int progress = 100;
                if (timeLeft < maxScale) {
                    progress = (int) (((float) timeLeft / maxScale) * 100);
                }

                if (progressCircleSecondary != null)
                    progressCircleSecondary.setProgress(progress);

                // Update every second
                countdownHandler.postDelayed(this, 1000);
            }
        };

        countdownHandler.post(countdownRunnable);
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

        // Apply if different
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != nightMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    private void setupSwipeAnimation() {
        PremiumSwipeCallback.SwipeActionListener listener = new PremiumSwipeCallback.SwipeActionListener() {
            @Override
            public void onSwipeRight(Task task, int position) {
                // Edit Action
                Intent intent = new Intent(DashboardActivity.this, NewTaskActivity.class);
                intent.putExtra(NewTaskActivity.EXTRA_ID, task.id);
                intent.putExtra(NewTaskActivity.EXTRA_TITLE, task.title);
                intent.putExtra(NewTaskActivity.EXTRA_DESCRIPTION, task.description);
                try {
                    intent.putExtra(NewTaskActivity.EXTRA_REMINDER_TIME, task.reminderTime);
                } catch (Exception e) {
                    // Ignore
                }
                intent.putExtra(NewTaskActivity.EXTRA_REMINDER_TYPE, task.reminderType);
                intent.putExtra(NewTaskActivity.EXTRA_REPEAT, task.repeat);
                // Removed EXTRA_URI and EXTRA_IS_COMPLETED as they don't exist

                taskActivityLauncher.launch(intent);
            }

            @Override
            public void onSwipeLeft(Task task, int position) {
                // Delete Action
                taskViewModel.softDelete(task);
                showUndoSnackbar(task);
            }
        };

        PremiumSwipeCallback.TaskAccessor accessor = new PremiumSwipeCallback.TaskAccessor() {
            @Override
            public Task getTaskAt(int position) {
                if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION
                        && position < dashboardShownTasks.size()) {
                    return dashboardShownTasks.get(position);
                }
                return null;
            }

            @Override
            public void notifyChanged(int position) {
                if (taskAdapter != null) {
                    taskAdapter.notifyItemChanged(position);
                }
            }
        };

        PremiumSwipeCallback swipeCallback = new PremiumSwipeCallback(this, accessor, listener);
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerTasks);
    }

    private void showUndoSnackbar(Task task) {
        String msg = "Task deleted";
        com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                msg,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    taskViewModel.restore(task);
                })
                .show();
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        // Update database
        task.isCompleted = isChecked;
        taskViewModel.update(task);

        // Show feedback
        String msg = isChecked ? "Task completed" : "Task activated";
        com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                msg,
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setAction("Undo", v -> {
                    task.isCompleted = !isChecked; // Revert
                    taskViewModel.update(task);
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countdownHandler.removeCallbacksAndMessages(null);
    }
}
