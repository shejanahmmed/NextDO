package com.shejan.nextdo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void schedule(Task task) {
        if (!areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled, skipping schedule for task " + task.id);
            return;
        }

        if (task.reminderTime > 0 && task.alarmId != 0) {
            // Cancel any existing alarm first
            cancel(task);

            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, task.title);
            intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, task.id);
            intent.putExtra("task_description", task.description);

            // CRITICAL FIX: Use FLAG_CANCEL_CURRENT to avoid PendingIntent reuse issues
            // FLAG_UPDATE_CURRENT can cause extras to be cached/reused incorrectly
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.alarmId, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                long currentTime = System.currentTimeMillis();
                long delayMs = task.reminderTime - currentTime;

                Log.d(TAG, "Scheduling alarm for task " + task.id + " at " + task.reminderTime +
                        " (in " + delayMs + "ms)");

                // For past times or very near times, schedule immediately
                if (delayMs <= 0) {
                    // Trigger immediately for past times
                    Log.d(TAG, "Reminder time in past, triggering immediately");
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTime + 100, pendingIntent);
                } else if (delayMs < 5000) {
                    // For times within 5 seconds, trigger immediately
                    Log.d(TAG, "Near-future reminder, triggering immediately");
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTime + 100, pendingIntent);
                } else if (canScheduleExactAlarms()) {
                    // Use most reliable alarm method for future times
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 12+: Use setAlarmClock for high-priority alarms
                        Log.d(TAG, "Using setAlarmClock for Android 12+");
                        alarmManager.setAlarmClock(
                                new AlarmManager.AlarmClockInfo(task.reminderTime, pendingIntent),
                                pendingIntent);
                    } else {
                        // Android 6-11: Use setExactAndAllowWhileIdle
                        Log.d(TAG, "Using setExactAndAllowWhileIdle for Android 6-11");
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime,
                                pendingIntent);
                    }
                } else {
                    // Fallback for devices without exact alarm permission
                    Log.d(TAG, "No exact alarm permission, using setAndAllowWhileIdle");
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent);
                }
                Log.d(TAG, "Alarm scheduled successfully for task " + task.id);
            } catch (Exception e) {
                // Fallback scheduling with retry
                Log.e(TAG, "Error scheduling alarm, using fallback: " + e.getMessage());
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent);
                    Log.d(TAG, "Fallback scheduling succeeded");
                } catch (Exception fallbackError) {
                    Log.e(TAG, "Fallback scheduling also failed: " + fallbackError.getMessage());
                }
            }
        } else {
            Log.w(TAG, "Invalid task: reminderTime=" + task.reminderTime + ", alarmId=" + task.alarmId);
        }
    }

    public void cancel(Task task) {
        if (task.alarmId != 0) {
            try {
                Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.alarmId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Alarm cancelled for task " + task.id);
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling alarm: " + e.getMessage());
            }
        }
    }

    private boolean areNotificationsEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("notifications", true);
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Permissions are not needed for earlier versions
    }
}
