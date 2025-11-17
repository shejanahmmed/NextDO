package com.shejan.nextdo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class AlarmScheduler {

    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void schedule(Task task) {
        if (!areNotificationsEnabled()) {
            return;
        }

        if (task.reminderTime > 0 && task.alarmId != 0) {
            // Cancel any existing alarm first
            cancel(task);

            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, task.title);
            intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, task.id);
            intent.putExtra("task_description", task.description);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                // For past or very near times, schedule immediately
                long delayMs = task.reminderTime - System.currentTimeMillis();
                if (delayMs < 1000) {
                    // Trigger immediately for past times or times within 1 second
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500,
                            pendingIntent);
                } else if (canScheduleExactAlarms()) {
                    // Use most reliable alarm method for future times
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime,
                                pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent);
                    }
                } else {
                    // Fallback for devices without exact alarm permission
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent);
                }
            } catch (Exception e) {
                // Fallback scheduling
                alarmManager.set(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent);
            }
        }
    }

    public void cancel(Task task) {
        if (task.alarmId != 0) {
            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
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
