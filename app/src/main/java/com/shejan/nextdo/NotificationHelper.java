package com.shejan.nextdo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    public static final String CHANNEL_ID = "nextdo_reminder_channel";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NextDO Reminders";
            String description = "Channel for NextDO task reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH; // DEFINITIVE FIX: Use HIGH importance for reminders.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
