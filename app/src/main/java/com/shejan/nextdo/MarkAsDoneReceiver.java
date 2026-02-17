package com.shejan.nextdo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

public class MarkAsDoneReceiver extends BroadcastReceiver {
    private static final String TAG = "MarkAsDoneReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, 0);
        String taskTitle = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE);

        if (taskId == 0) {
            Log.e(TAG, "Invalid taskId");
            return;
        }

        // Dismiss the notification immediately
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }

        // Update Task in Background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                TaskDao taskDao = db.taskDao();
                Task task = taskDao.getTaskById(taskId);

                if (task != null) {
                    // Mark as completed
                    task.isCompleted = true;
                    taskDao.update(task);

                    // Cancel any scheduled alarms for this task
                    AlarmScheduler alarmScheduler = new AlarmScheduler(context);
                    alarmScheduler.cancel(task);

                    // Notify widgets/UI
                    UpcomingTasksWidgetProvider.sendRefreshBroadcast(context);

                    Log.d(TAG, "Task " + taskId + " marked as done from notification");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking task as done: " + e.getMessage());
            }
        });

        // Show feedback to user
        Toast.makeText(context, "Task marked as done", Toast.LENGTH_SHORT).show();
    }
}
