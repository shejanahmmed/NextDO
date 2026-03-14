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
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements DashboardTaskAdapter.OnTaskActionListener {

    // New Animation Views
    private View layoutHeaderExpanded;
    private View layoutHeaderCollapsed;
    private TextView textHeaderCount;
    private TextView textHeaderCountDone;
    private TextView textHeaderPill;
    private AppBarLayout appBarLayout;

    private TextView textTimelineHeader;
    private TextView textEmptyState;

    // Existing fields restored
    private RecyclerView recyclerDates;
    private RecyclerView recyclerTasks;
    private DashboardDateAdapter dateAdapter;
    private DashboardTaskAdapter taskAdapter;
    private FloatingActionButton fab;
    private ProgressBar progressCircle;

    private TextView textSummary;
    private TextView textProgressPercent;

    private TaskViewModel taskViewModel;

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

    // Data tracking
    private Calendar selectedDate = Calendar.getInstance();
    private List<Task> currentActiveTasks = new ArrayList<>();
    private List<Task> currentCompletedTasks = new ArrayList<>();
    private List<Task> dashboardShownTasks = new ArrayList<>(); // Track tasks shown in adapter
    private int currentlySwipedPosition = -1; // Track swipe by position for stability

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyThemePreference();
        setContentView(R.layout.activity_dashboard);

        // Enable Edge-to-Edge so content flows under the status bar natively
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Initialize Views
        recyclerDates = findViewById(R.id.recycler_dates);
        recyclerTasks = findViewById(R.id.recycler_tasks);
        fab = findViewById(R.id.fab_dashboard);
        progressCircle = findViewById(R.id.progress_bar_circle);

        textSummary = findViewById(R.id.text_summary_line1);
        textProgressPercent = findViewById(R.id.text_progress_percent);
        TextView textMonthYear = findViewById(R.id.text_month_year);
        if (textMonthYear != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault());
            textMonthYear.setText(sdf.format(selectedDate.getTime()));
        }
        TextView textViewAll = findViewById(R.id.text_view_all);
        if (textViewAll != null) {
            textViewAll.setOnClickListener(v -> showCalendarPicker());
        }

        // New Animation Views
        layoutHeaderExpanded = findViewById(R.id.layout_header_expanded);
        layoutHeaderCollapsed = findViewById(R.id.layout_header_collapsed);
        textHeaderCount = findViewById(R.id.text_header_count);
        textHeaderCountDone = findViewById(R.id.text_header_count_done);
        textHeaderPill = findViewById(R.id.text_header_pill);

        // Timeline Header Views for Scroll Effect
        textTimelineHeader = findViewById(R.id.text_timeline_header);
        textEmptyState = findViewById(R.id.text_empty_state);

        // --- Setup Adapters & Listeners ---

        // Bottom Navigation clicks
        if (fab != null) {
            fab.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, NewTaskActivity.class);
                startActivity(intent);
            });
        }
        android.view.View navDashboard = findViewById(R.id.nav_dashboard);
        if (navDashboard != null)
            navDashboard.setOnClickListener(v -> {
                /* already here */ });

        android.view.View navReminders = findViewById(R.id.nav_reminders);
        if (navReminders != null)
            navReminders.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                startActivity(intent);
            });

        android.view.View navCompleted = findViewById(R.id.nav_completed);
        if (navCompleted != null)
            navCompleted.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, CompletedTasksActivity.class);
                startActivity(intent);
            });

        android.view.View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null)
            navSettings.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, SettingsActivity.class);
                startActivity(intent);
            });

        // Setup Date Scroller (Horizontal)
        recyclerDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dateAdapter = new DashboardDateAdapter();
        dateAdapter.setDates(generateDates());
        dateAdapter.setOnDateClickListener(new DashboardDateAdapter.OnDateClickListener() {
            @Override
            public void onDateClicked(DashboardDateAdapter.DateItem date, int position) {
                selectedDate.setTimeInMillis(date.timestamp);
                dateAdapter.setSelectedDate(date.timestamp);

                // Update UI for the selected date
                TextView txtMonth = findViewById(R.id.text_month_year);
                if (txtMonth != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy",
                            java.util.Locale.getDefault());
                    txtMonth.setText(sdf.format(selectedDate.getTime()));
                }
                filterAndShowTasks();
            }

            @Override
            public void onArrowClicked() {
                showCalendarPicker();
            }
        });
        recyclerDates.setAdapter(dateAdapter);

        // Scroll to Today effectively
        recyclerDates.post(() -> {
            if (recyclerDates.getLayoutManager() != null) {
                ((LinearLayoutManager) recyclerDates.getLayoutManager()).scrollToPositionWithOffset(16, 0); // Index 15
                                                                                                            // + 1 (Left
                                                                                                            // Arrow)
            }
        }); // (index 15) at
        // start
        // Let's scroll to 12.

        // Setup Timeline Tasks (Vertical)
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new DashboardTaskAdapter(this);
        recyclerTasks.setAdapter(taskAdapter);

        // Enable nested scrolling so task list scroll collapses the header first
        recyclerTasks.setNestedScrollingEnabled(true);

        // FAB Setup
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, NewTaskActivity.class);
            taskActivityLauncher.launch(intent);
        });

        // Initialize ViewModel & AlarmScheduler
        alarmScheduler = new AlarmScheduler(this);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Observe Tasks
        taskViewModel.getActiveTasks().observe(this, tasks -> {
            currentActiveTasks = tasks;
            updateDashboard(currentActiveTasks, currentCompletedTasks);
        });

        taskViewModel.getCompletedTasks().observe(this, tasks -> {
            currentCompletedTasks = tasks;
            updateDashboard(currentActiveTasks, currentCompletedTasks);
        });

        // Initialize CoordinatorLayout behavior
        appBarLayout = findViewById(R.id.app_bar);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);

        // Ensure initial state
        if (layoutHeaderCollapsed != null) {
            layoutHeaderCollapsed.setAlpha(0f);
            layoutHeaderCollapsed.setVisibility(View.INVISIBLE);
        }

        if (appBarLayout != null && collapsingToolbar != null) {
            final Handler snapHandler = new Handler(Looper.getMainLooper());
            final Runnable[] snapRunnable = { null };
            final float[] lastPercentage = { 0f };

            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    int totalScrollRange = appBarLayout.getTotalScrollRange();
                    if (totalScrollRange == 0)
                        return;

                    float percentage = (float) Math.abs(verticalOffset) / (float) totalScrollRange;
                    lastPercentage[0] = percentage;

                    // Fade out the Expanded Header (Faster, to avoid overlap with Title)
                    if (layoutHeaderExpanded != null) {
                        float fadeOutLimit = 0.6f; // Fully invisible by 60% scroll
                        float alpha = 1f - (percentage / fadeOutLimit);
                        layoutHeaderExpanded.setAlpha(Math.max(0f, alpha));
                        layoutHeaderExpanded.setVisibility(alpha > 0 ? View.VISIBLE : View.INVISIBLE);
                    }

                    // Fade in the Collapsed Header (Sticky Card)
                    if (layoutHeaderCollapsed != null) {
                        layoutHeaderCollapsed.setAlpha(percentage);
                        layoutHeaderCollapsed.setVisibility(percentage > 0.1f ? View.VISIBLE : View.INVISIBLE);
                    }

                    // Dynamically set empty state padding to keep it centered in the visible
                    // timeline area
                    if (textEmptyState != null) {
                        int baseBottomPadding = (int) (100 * getResources().getDisplayMetrics().density); // bottom nav
                                                                                                          // + padding
                        int offscreenAmount = totalScrollRange - Math.abs(verticalOffset);
                        textEmptyState.setPadding(0, 0, 0, baseBottomPadding + offscreenAmount);
                    }

                    // Quick-snap: debounce scroll stop, then snap at 25% threshold
                    if (snapRunnable[0] != null)
                        snapHandler.removeCallbacks(snapRunnable[0]);
                    snapRunnable[0] = () -> {
                        if (lastPercentage[0] > 0.25f && lastPercentage[0] < 1.0f) {
                            appBarLayout.setExpanded(false, true);
                        } else if (lastPercentage[0] > 0f && lastPercentage[0] <= 0.25f) {
                            appBarLayout.setExpanded(true, true);
                        }
                    };
                    snapHandler.postDelayed(snapRunnable[0], 200);
                }
            });

            // Calculate Minimum Height for Collapsing Toolbar to ensure Sticky Stack fits
            // Stack = Title Spacer (80dp) + Header Card (wrap) + Timeline Header (wrap)
            // We need to measure them to be exact.

            collapsingToolbar.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            collapsingToolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            int titleSpacerHeight = (int) (45 * getResources().getDisplayMetrics().density); // 45dp Top
                                                                                                             // gap
                                                                                                             // matching
                                                                                                             // XML
                            int bottomGapSpacer = (int) (60 * getResources().getDisplayMetrics().density); // 60dp
                                                                                                           // Bottom
                                                                                                           // explicit
                                                                                                           // gap

                            int headerCardHeight = (layoutHeaderCollapsed != null) ? layoutHeaderCollapsed.getHeight()
                                    : 0;
                            if (headerCardHeight == 0)
                                headerCardHeight = (int) (74 * getResources().getDisplayMetrics().density);

                            View timelineHeader = findViewById(R.id.text_timeline_header);
                            int timelineHeaderHeight = (timelineHeader != null) ? timelineHeader.getHeight() : 0;
                            if (timelineHeaderHeight == 0)
                                timelineHeaderHeight = (int) (50 * getResources().getDisplayMetrics().density);

                            // Calculate total pinned height using symmetric 60dp gaps around the sticky
                            // header card
                            int totalPinnedHeight = titleSpacerHeight + headerCardHeight + bottomGapSpacer
                                    + timelineHeaderHeight;

                            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
                            if (toolbar != null) {
                                toolbar.setMinimumHeight(totalPinnedHeight);
                                collapsingToolbar.setMinimumHeight(totalPinnedHeight);
                                collapsingToolbar.setScrimVisibleHeightTrigger(totalPinnedHeight);
                            }
                        }
                    });
        }

        // ... (Rest of onCreate)
    }

    // ... (drawer setup)

    private void updateDashboard(List<Task> activeTasks, List<Task> completedTasks) {
        if (activeTasks == null)
            activeTasks = new ArrayList<>();
        if (completedTasks == null)
            completedTasks = new ArrayList<>();

        // ... (Calculation logic same as before)
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        int todayTotal = 0;
        int todayCompleted = 0;

        // Filter Active Tasks for Today
        for (Task task : activeTasks) {
            calendar.setTimeInMillis(task.reminderTime);
            if (task.reminderTime > 0 && calendar.get(Calendar.YEAR) == year
                    && calendar.get(Calendar.DAY_OF_YEAR) == dayOfYear) {
                todayTotal++;
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
            String summaryText = todayCompleted + " Done, <font color='#c7082f'><b>" + todayLeft + " Left</b></font>";
            textSummary.setText(Html.fromHtml(summaryText, Html.FROM_HTML_MODE_LEGACY));
        }

        // Update Header Count
        if (textHeaderCount != null) {
            textHeaderCount.setText(String.valueOf(todayLeft));
        }

        if (textHeaderCountDone != null) {
            textHeaderCountDone.setText(String.valueOf(todayCompleted));
        }

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
        View emptyStateScroll = findViewById(R.id.empty_state_scroll);
        if (filteredTasks.isEmpty()) {
            recyclerTasks.setVisibility(View.GONE);
            if (emptyStateScroll != null)
                emptyStateScroll.setVisibility(View.VISIBLE);
        } else {
            recyclerTasks.setVisibility(View.VISIBLE);
            if (emptyStateScroll != null)
                emptyStateScroll.setVisibility(View.GONE);
        }

        // Sort by time (ASC) - Upcoming soon at top
        java.util.Collections.sort(filteredTasks, (t1, t2) -> Long.compare(t1.reminderTime, t2.reminderTime));

        this.dashboardShownTasks = filteredTasks;
        taskAdapter.setTasks(filteredTasks);
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

    @Override
    public void onTaskEdit(Task task) {
        // Edit Action
        Intent intent = new Intent(DashboardActivity.this, NewTaskActivity.class);
        intent.putExtra(NewTaskActivity.EXTRA_ID, task.id);
        intent.putExtra(NewTaskActivity.EXTRA_ALARM_ID, task.alarmId);
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
    public void onTaskDelete(Task task) {
        taskViewModel.delete(task);
        com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "Task deleted",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setAction("Undo", v -> taskViewModel.insert(task))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showCalendarPicker() {
        ModernCalendarBottomSheet calendarSheet = ModernCalendarBottomSheet
                .newInstance(selectedDate.getTimeInMillis());
        calendarSheet.setOnDateSelectedListener(dateInMillis -> {
            Intent intent = new Intent(DashboardActivity.this, DailyRemindersActivity.class);
            intent.putExtra("SELECTED_TIMESTAMP", dateInMillis);
            startActivity(intent);
        });
        calendarSheet.show(getSupportFragmentManager(), "calendar_sheet");
    }

    private int blendColors(int from, int to, float ratio) {
        float inverseRatio = 1f - ratio;
        float a = android.graphics.Color.alpha(to) * ratio + android.graphics.Color.alpha(from) * inverseRatio;
        float r = android.graphics.Color.red(to) * ratio + android.graphics.Color.red(from) * inverseRatio;
        float g = android.graphics.Color.green(to) * ratio + android.graphics.Color.green(from) * inverseRatio;
        float b = android.graphics.Color.blue(to) * ratio + android.graphics.Color.blue(from) * inverseRatio;
        return android.graphics.Color.argb((int) a, (int) r, (int) g, (int) b);
    }
}
