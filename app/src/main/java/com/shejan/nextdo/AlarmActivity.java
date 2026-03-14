package com.shejan.nextdo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm);

        // Set status bar color to match background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.alarm_background_color));
        }

        // Get task details from intent
        String taskTitle = getIntent().getStringExtra("task_title");
        String taskDescription = getIntent().getStringExtra("task_description");
        int taskId = getIntent().getIntExtra("task_id", 0);

        // Setup UI
        TextView titleText = findViewById(R.id.alarm_title);
        TextView descText = findViewById(R.id.alarm_description);
        Button doneButton = findViewById(R.id.btn_done);
        Button snoozeButton = findViewById(R.id.btn_snooze);

        titleText.setText(taskTitle != null ? taskTitle : "Reminder");
        // Description is now hidden by default in XML as requested
        descText.setVisibility(android.view.View.GONE);

        // NOTE: Sound and vibration are managed by the notification system (FLAG_INSISTENT)
        // launched via ReminderBroadcastReceiver. cancelNotification() below stops it.

        // Done button (Mark as Done logic)
        doneButton.setOnClickListener(v -> {
            markTaskAsDone(taskId);
        });

        // Snooze button
        snoozeButton.setOnClickListener(v -> {
            // CRITICAL: Check if task is still valid (not deleted) before snoozing
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    Task task = db.taskDao().getTaskById(taskId);
                    if (task == null || task.isDeleted) {
                        runOnUiThread(() -> {
                            cancelNotification(taskId);
                            android.widget.Toast.makeText(this, "This reminder has been deleted",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }

                    // Proceed with snooze
                    runOnUiThread(() -> {
                        cancelNotification(taskId);

                        // Send broadcast to SnoozeReceiver
                        Intent snoozeIntent = new Intent(this, SnoozeReceiver.class);
                        snoozeIntent.putExtra("task_id", taskId);
                        snoozeIntent.putExtra("task_title", taskTitle);
                        snoozeIntent.putExtra("task_description", taskDescription);
                        snoozeIntent.putExtra("alarm_id", getIntent().getIntExtra("alarm_id", 0));
                        snoozeIntent.putExtra("reminder_type", "alarm");
                        sendBroadcast(snoozeIntent);

                        finish();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error checking task status: " + e.getMessage());
                    runOnUiThread(() -> {
                        cancelNotification(taskId);
                        finish();
                    });
                }
            }).start();
        });

        // Handle back button press - prevent dismissing alarm
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent back button from dismissing alarm
                // User must explicitly dismiss or snooze
            }
        });
    }

    private void markTaskAsDone(int taskId) {
        cancelNotification(taskId);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                TaskDao taskDao = db.taskDao();
                Task task = taskDao.getTaskById(taskId);

                if (task != null) {
                    // Mark as completed
                    task.isCompleted = true;
                    task.completedTimestamp = System.currentTimeMillis();
                    taskDao.update(task);

                    // Cancel any scheduled alarms for this task
                    AlarmScheduler alarmScheduler = new AlarmScheduler(this);
                    alarmScheduler.cancel(task);

                    // Notify widgets/UI
                    UpcomingTasksWidgetProvider.sendRefreshBroadcast(this);

                    Log.d(TAG, "Task " + taskId + " marked as done from Alarm Screen");
                    
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Task marked as done", android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(this::finish);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking task as done: " + e.getMessage());
                runOnUiThread(this::finish);
            }
        });
    }

    private void cancelNotification(int taskId) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(taskId);
                Log.d(TAG, "Alarm notification " + taskId + " cancelled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Notification is already cancelled in dismiss/snooze handlers
    }
}
