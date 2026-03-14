package com.shejan.nextdo;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class SnoozeReceiver extends BroadcastReceiver {
    private static final String TAG = "SnoozeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, 0);
        String taskTitle = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE);
        String taskDescription = intent.getStringExtra("task_description");

        if (taskId == 0) {
            Log.e(TAG, "Invalid taskId");
            return;
        }

        // Dismiss the current notification
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }

        final PendingResult pendingResult = goAsync();
        // Process in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // CRITICAL: Check if task still exists and is not deleted
                AppDatabase db = AppDatabase.getDatabase(context);
                Task task = db.taskDao().getTaskById(taskId);
                if (task == null || task.isDeleted || task.isCompleted) {
                    Log.d(TAG, "Task " + taskId + " is deleted/completed, not snoozing");
                    return;
                }

                // Get snooze duration from preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String durationStr = prefs.getString("snooze_duration", "300000"); // Default 5 mins
                long duration = Long.parseLong(durationStr);

                // Schedule new alarm
                long triggerTime = System.currentTimeMillis() + duration;

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    Intent reminderIntent = new Intent(context, ReminderBroadcastReceiver.class);
                    reminderIntent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, taskTitle);
                    reminderIntent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, taskId);
                    reminderIntent.putExtra("alarm_id", intent.getIntExtra("alarm_id", 0));
                    reminderIntent.putExtra("task_description", taskDescription);

                    String reminderType = intent.getStringExtra("reminder_type");
                    reminderIntent.putExtra("reminder_type", reminderType);

                    int alarmId = intent.getIntExtra("alarm_id", 0);
                    int requestCode = (alarmId != 0) ? alarmId : taskId;

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, reminderIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    }

                    Log.d(TAG, "Snoozed task " + taskId + " for " + duration + "ms");
                    
                    // Show toast on UI thread
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        long minutes = duration / 60000;
                        Toast.makeText(context, "Snoozed for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during snooze: " + e.getMessage());
            } finally {
                pendingResult.finish();
            }
        });
    }
}
