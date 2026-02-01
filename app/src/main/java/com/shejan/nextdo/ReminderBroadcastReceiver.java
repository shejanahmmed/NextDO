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
        // CRITICAL FIX: Prevent duplicate broadcasts within 1 second
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime < 1000) {
            Log.d(TAG, "Duplicate broadcast detected within 1 second, ignoring");
            return; // Ignore if called again within 1 second
        }
        lastNotificationTime = currentTime;

        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                Log.d(TAG, "Alarm received, processing in background");

                String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
                String taskDescription = intent.getStringExtra("task_description");
                int taskId = intent.getIntExtra(EXTRA_TASK_ID, 0);

                if (taskId == 0) {
                    Log.e(TAG, "Invalid taskId, aborting notification");
                    return;
                }

                // Check DB for completion and reschedule
                try {
                    AppDatabase db = AppDatabase.getDatabase(context);
                    Task foundTask = db.taskDao().getTaskById(taskId);

                    if (foundTask != null && foundTask.isCompleted) {
                        Log.d(TAG, "Task " + taskId + " is already completed, not showing notification");
                        return;
                    }

                    // Auto-Reschedule logic for repeating tasks
                    if (foundTask != null && !android.text.TextUtils.isEmpty(foundTask.repeat)) {
                        long nextTime = AlarmScheduler.getNextOccurrence(System.currentTimeMillis(),
                                foundTask.reminderTime, foundTask.repeat);
                        if (nextTime > System.currentTimeMillis()) {
                            foundTask.reminderTime = nextTime;
                            db.taskDao().update(foundTask);

                            // Schedule the next alarm
                            new AlarmScheduler(context).schedule(foundTask);
                            Log.d(TAG, "Auto-rescheduled task " + taskId + " to " + nextTime);
                        } else {
                            Log.d(TAG, "No valid future occurrence found for repeat pattern: " + foundTask.repeat);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Database error in receiver: " + e.getMessage());
                }

                // Show Notification
                Log.d(TAG, "Showing notification for task " + taskId);

                Intent mainIntent = new Intent(context, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId, mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                String contentText = taskTitle != null ? taskTitle : "You have a reminder";
                if (taskDescription != null && !taskDescription.isEmpty()) {
                    contentText = taskTitle + ": " + taskDescription;
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                        NotificationHelper.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_alarm)
                        .setContentTitle("NextDO Reminder")
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                // Add Snooze Action
                int alarmId = intent.getIntExtra("alarm_id", 0);
                Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
                snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);
                snoozeIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
                snoozeIntent.putExtra("alarm_id", alarmId);
                snoozeIntent.putExtra("task_description", taskDescription);
                PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, taskId + 20000, snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                builder.addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent);

                // Standard notification behavior
                builder.setOngoing(false)
                        .setOnlyAlertOnce(true)
                        .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND
                                | NotificationCompat.DEFAULT_VIBRATE)
                        .setSound(android.media.RingtoneManager
                                .getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                        .setVibrate(new long[] { 0, 500, 250, 500 });

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                if (ActivityCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(taskId, builder.build());
                    Log.d(TAG, "Notification displayed successfully for task " + taskId);
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in background receiver: " + e.getMessage(), e);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
