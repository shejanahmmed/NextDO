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
            reminderIntent.putExtra("alarm_id", intent.getIntExtra("alarm_id", 0)); // Pass it forward again
            reminderIntent.putExtra("task_description", taskDescription);

            // Use the original alarmId if available, otherwise fallback to taskId
            int alarmId = intent.getIntExtra("alarm_id", 0);
            int requestCode = (alarmId != 0) ? alarmId : taskId;

            // We use the same ID as the original alarm to overwrite it effectively for this
            // task
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }

                // Calculate minutes for toast message
                long minutes = duration / 60000;
                Toast.makeText(context, "Snoozed for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Snoozed task " + taskId + " for " + duration + "ms");
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error scheduling snooze: " + e.getMessage());
                Toast.makeText(context, "Failed to snooze: Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
