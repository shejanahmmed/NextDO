package com.shejan.nextdo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

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
                String reminderType = intent.getStringExtra("reminder_type"); // CRITICAL: Get from intent

                Log.d(TAG, "TRIGGER DEBUG: Received reminderType from intent: '" + reminderType + "'");

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

                // Handle voice reminders BEFORE showing notification
                if ("voice".equals(reminderType)) {
                    Log.d(TAG, "Voice reminder detected from intent for task " + taskId);
                    // Get full task from database for voice playback
                    try {
                        AppDatabase db = AppDatabase.getDatabase(context);
                        Task foundTask = db.taskDao().getTaskById(taskId);
                        if (foundTask != null) {
                            speakVoiceReminder(context, foundTask);
                            return; // Don't show notification for voice reminders
                        } else {
                            Log.w(TAG, "Task not found in database, cannot speak");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error loading task for voice: " + e.getMessage());
                    }
                }

                // Show Notification (for notification and alarm reminders)
                Log.d(TAG, "Showing notification for task " + taskId + " with type: " + reminderType);

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
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                // For ALARM type: Launch full-screen AlarmActivity (primary method)
                if ("alarm".equals(reminderType)) {
                    Log.d(TAG, "Launching AlarmActivity for task " + taskId);
                    Intent alarmIntent = new Intent(context, AlarmActivity.class);
                    alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    alarmIntent.putExtra("task_id", taskId);
                    alarmIntent.putExtra("task_title", taskTitle);
                    alarmIntent.putExtra("task_description", taskDescription);
                    alarmIntent.putExtra("alarm_id", intent.getIntExtra("alarm_id", 0));

                    try {
                        context.startActivity(alarmIntent);
                        // Don't show notification for alarm type - activity handles it
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to launch AlarmActivity: " + e.getMessage());
                        // Fall through to show notification as backup
                    }
                }

                // For ALARM type: Add full-screen intent to show over lockscreen
                if ("alarm".equals(reminderType)) {
                    Log.d(TAG, "Setting up full-screen alarm for task " + taskId);
                    Intent fullScreenIntent = new Intent(context, MainActivity.class);
                    fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, taskId + 10000,
                            fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    builder.setFullScreenIntent(fullScreenPendingIntent, true)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                            .setVibrate(new long[] { 0, 1000, 500, 1000 });
                }

                // Add Snooze Action
                int alarmId = intent.getIntExtra("alarm_id", 0);
                Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
                snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);
                snoozeIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
                snoozeIntent.putExtra("alarm_id", alarmId);
                snoozeIntent.putExtra("task_description", taskDescription);
                snoozeIntent.putExtra("reminder_type", reminderType);
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

    private void speakVoiceReminder(Context context, Task task) {
        final TextToSpeech[] ttsWrapper = new TextToSpeech[1];
        ttsWrapper[0] = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                TextToSpeech tts = ttsWrapper[0];
                tts.setLanguage(Locale.US);

                // Set male voice
                setMaleVoice(tts);

                String textToSpeak = "Reminder: " + task.title;
                if (task.description != null && !task.description.isEmpty()) {
                    textToSpeak += ". " + task.description;
                }

                Log.d(TAG, "Speaking voice reminder: " + textToSpeak);
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "reminder");

                // Cleanup after speaking (give enough time for speech)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    tts.stop();
                    tts.shutdown();
                }, 10000); // 10 seconds should be enough for most reminders
            } else {
                Log.e(TAG, "TTS initialization failed, falling back to notification");
                // Fallback to showing notification if TTS fails
                showNotificationForVoiceReminder(context, task);
            }
        });
    }

    private void setMaleVoice(TextToSpeech tts) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                java.util.Set<android.speech.tts.Voice> voices = tts.getVoices();
                if (voices != null) {
                    for (android.speech.tts.Voice voice : voices) {
                        // Look for English (US) Male voice
                        if (voice.getLocale().equals(Locale.US)) {
                            String voiceName = voice.getName().toLowerCase();
                            if (voiceName.contains("male") && !voiceName.contains("female")) {
                                tts.setVoice(voice);
                                Log.d(TAG, "Selected male voice: " + voice.getName());
                                return;
                            }
                        }
                    }

                    // Fallback: try any English male voice
                    for (android.speech.tts.Voice voice : voices) {
                        if (voice.getLocale().getLanguage().equals("en")) {
                            String voiceName = voice.getName().toLowerCase();
                            if (voiceName.contains("male") && !voiceName.contains("female")) {
                                tts.setVoice(voice);
                                Log.d(TAG, "Selected fallback male voice: " + voice.getName());
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting male voice: " + e.getMessage());
            }
        }
    }

    private void showNotificationForVoiceReminder(Context context, Task task) {
        // Simple notification as fallback when TTS fails
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, task.id, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contentText = "Voice Reminder: " + task.title;
        if (task.description != null && !task.description.isEmpty()) {
            contentText += ": " + task.description;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("NextDO Voice Reminder")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(task.id, builder.build());
            Log.d(TAG, "Fallback notification shown for voice reminder");
        }
    }
}
