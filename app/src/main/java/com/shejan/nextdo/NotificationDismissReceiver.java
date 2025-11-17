package com.shejan.nextdo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class NotificationDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, 0);

        // Cancel the persistent notification when user swipes it
        if (taskId > 0) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(taskId);
        }
    }
}