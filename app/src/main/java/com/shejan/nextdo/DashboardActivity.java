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

public class DashboardActivity extends AppCompatActivity {

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

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private TaskViewModel taskViewModel;
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Task nextUpcomingTask = null;
    private Runnable countdownRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        taskAdapter = new DashboardTaskAdapter();
        recyclerTasks.setAdapter(taskAdapter);

        // Setup FAB
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, NewTaskActivity.class);
            startActivity(intent);
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
            updateDashboard(activeTasks, taskViewModel.getCompletedTasks().getValue());
        });

        taskViewModel.getCompletedTasks().observe(this, completedTasks -> {
            updateDashboard(taskViewModel.getActiveTasks().getValue(), completedTasks);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countdownHandler.removeCallbacksAndMessages(null);
    }
}
