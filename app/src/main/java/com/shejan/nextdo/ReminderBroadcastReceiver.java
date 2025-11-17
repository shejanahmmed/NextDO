package com.shejan.nextdo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderBroadcastReceiver";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_ID = "task_id";
    private static long lastNotificationTime = 0; // CRITICAL FIX: Prevent duplicate broadcasts

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(TAG, "Alarm received for notification");

            String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
            String taskDescription = intent.getStringExtra("task_description");
            int taskId = intent.getIntExtra(EXTRA_TASK_ID, 0);

            // CRITICAL FIX: Prevent duplicate broadcasts within 1 second
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNotificationTime < 1000) {
                Log.d(TAG, "Duplicate broadcast detected within 1 second, ignoring");
                return; // Ignore if called again within 1 second
            }
            lastNotificationTime = currentTime;

            if (taskId == 0) {
                Log.e(TAG, "Invalid taskId, aborting notification");
                return;
            }

            // CRITICAL FIX: Check if task is already completed before showing notification
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                java.util.List<Task> allTasks = db.taskDao().getAllTasksSync();
                Task foundTask = null;
                for (Task t : allTasks) {
                    if (t.id == taskId) {
                        foundTask = t;
                        break;
                    }
                }

                if (foundTask != null && foundTask.isCompleted) {
                    Log.d(TAG, "Task " + taskId + " is already completed, not showing notification");
                    return; // Don't show notification for completed tasks
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not check if task is completed: " + e.getMessage());
                // Continue anyway - better to show notification than skip it
            }

            Log.d(TAG, "Showing notification for task " + taskId);

            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId, mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent deleteIntent = new Intent(context, NotificationDismissReceiver.class);
            deleteIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
            deleteIntent.putExtra("task_description", taskDescription);
            deleteIntent.putExtra(EXTRA_TASK_ID, taskId);
            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, taskId + 10000, deleteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String contentText = taskTitle != null ? taskTitle : "You have a reminder";
            if (taskDescription != null && !taskDescription.isEmpty()) {
                contentText = taskTitle + ": " + taskDescription;
            }

            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                    .getDefaultSharedPreferences(context);
            boolean persistentEnabled = prefs.getBoolean("persistent_notifications", false);

            Log.d(TAG, "Persistent notifications enabled: " + persistentEnabled);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle("NextDO Reminder")
                    .setContentText(contentText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(!persistentEnabled)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // Handle persistent vs regular notification differently
            if (persistentEnabled) {
                // For persistent notifications: keep them ongoing but still alert
                builder.setOngoing(true)
                        .setDeleteIntent(deletePendingIntent)
                        .setOnlyAlertOnce(false) // CRITICAL: Allow alerts even for persistent
                        .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND
                                | NotificationCompat.DEFAULT_VIBRATE)
                        .setSound(android.media.RingtoneManager
                                .getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                        .setVibrate(new long[] { 0, 500, 250, 500 });
                Log.d(TAG, "Persistent notification: sound and vibration enabled");
            } else {
                // For regular notifications: auto-cancel and alert once
                builder.setOngoing(false)
                        .setOnlyAlertOnce(true)
                        .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND
                                | NotificationCompat.DEFAULT_VIBRATE)
                        .setSound(android.media.RingtoneManager
                                .getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                        .setVibrate(new long[] { 0, 500, 250, 500 });
                Log.d(TAG, "Regular notification: auto-cancel enabled");
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(taskId, builder.build());
                Log.d(TAG, "Notification displayed successfully for task " + taskId);
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage(), e);
        }
    }
}
