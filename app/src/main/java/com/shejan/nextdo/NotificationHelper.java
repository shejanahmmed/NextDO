package com.shejan.nextdo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "nextdo_reminder_channel";
    public static final String ALARM_CHANNEL_ID = "nextdo_alarm_channel_v2"; // v2: Fresh ID with explicit sound
    public static final String VOICE_CHANNEL_ID = "nextdo_voice_channel_v1"; // Silent channel for voice

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Regular Reminder Channel
                NotificationChannel reminderChannel = getNotificationChannel();
                notificationManager.createNotificationChannel(reminderChannel);
                
                // Dedicated Alarm Channel
                NotificationChannel alarmChannel = getAlarmNotificationChannel();
                notificationManager.createNotificationChannel(alarmChannel);

                // Dedicated Silent Voice Channel
                NotificationChannel voiceChannel = getVoiceNotificationChannel();
                notificationManager.createNotificationChannel(voiceChannel);
                
                Log.d(TAG, "Notification channels created successfully");
            }
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel getNotificationChannel() {
        CharSequence name = "NextDO Reminders";
        String description = "Notifications for task reminders";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return channel;
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    @android.annotation.SuppressLint("WrongConstant")
    private static NotificationChannel getAlarmNotificationChannel() {
        CharSequence name = "NextDO Alarms";
        String description = "High priority full-screen alerts for alarms";
        int importance = NotificationManager.IMPORTANCE_MAX; // CRITICAL: MAX for full-screen intents
        NotificationChannel channel = new NotificationChannel(ALARM_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[] { 0, 1000, 500, 1000, 500, 1000 });
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // CRITICAL: Set alarm sound on the channel itself
        android.net.Uri alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
        }
        
        channel.setSound(alarmSound, new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());

        return channel;
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    @android.annotation.SuppressLint("WrongConstant")
    private static NotificationChannel getVoiceNotificationChannel() {
        CharSequence name = "NextDO Voice Reminders";
        String description = "Silent channel for voice reminders to prevent overlapping sounds";
        int importance = NotificationManager.IMPORTANCE_MAX; // CRITICAL: MAX for full-screen intents
        NotificationChannel channel = new NotificationChannel(VOICE_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // CRITICAL: Set NO sound for this channel
        channel.setSound(null, null);

        // CRITICAL: Alarms should bypass DND if possible
        if (Build.VERSION.SDK_INT >= 33) {
            channel.setBlockable(false);
        }
        return channel;
    }
}
