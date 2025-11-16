package com.shejan.nextdo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_ID = "task_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, 0);

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("NextDO Reminder")
                .setContentText(taskTitle != null ? taskTitle : "You have a reminder.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(taskId, builder.build());
        }
    }
}
