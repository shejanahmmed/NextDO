package com.shejan.nextdo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
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
            return; // Do not schedule if notifications are disabled
        }

        if (task.reminderTime > System.currentTimeMillis() && task.alarmId != 0) {
            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, task.title);
            intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, task.id);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent);
            } else {
                // Guide user to settings to grant permission
                Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                context.startActivity(settingsIntent);
                Toast.makeText(context, "Please grant permission to schedule exact alarms.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void cancel(Task task) {
        if (task.alarmId != 0) {
            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
