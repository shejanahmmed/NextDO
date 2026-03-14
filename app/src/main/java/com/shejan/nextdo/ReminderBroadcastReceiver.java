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

                if (taskId == 0) {
                    Log.e(TAG, "Invalid taskId, aborting notification");
                    return;
                }

                // Check DB for completion and reschedule
                AppDatabase db = null;
                Task foundTask = null;
                try {
                    db = AppDatabase.getDatabase(context);
                    TaskDao taskDao = db.taskDao();
                    foundTask = taskDao.getTaskById(taskId);
                    
                    if (foundTask != null) {
                        Log.d(TAG, "Found task in DB: " + foundTask.title + " with type: " + foundTask.reminderType);
                        
                        if (foundTask.isCompleted || foundTask.isDeleted) {
                            Log.d(TAG, "Task " + taskId + " is already completed or deleted, not showing notification");
                            return;
                        }

                        // Auto-Reschedule logic for repeating tasks
                        if (!android.text.TextUtils.isEmpty(foundTask.repeat)) {
                            long nextTime = AlarmScheduler.getNextOccurrence(System.currentTimeMillis(),
                                    foundTask.reminderTime, foundTask.repeat);
                            if (nextTime > System.currentTimeMillis()) {
                                foundTask.reminderTime = nextTime;
                                foundTask.isCompleted = false;
                                foundTask.completedTimestamp = 0;
                                taskDao.update(foundTask);
                                new AlarmScheduler(context).schedule(foundTask);
                                Log.d(TAG, "Auto-rescheduled task " + taskId + " to " + nextTime);
                            }
                        }
                    } else {
                        Log.w(TAG, "Task with ID " + taskId + " NOT found in database!");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Database access failed in receiver: " + e.getMessage());
                }

                // Ensure channels are created (especially the new v2 alarm channel)
                NotificationHelper.createNotificationChannel(context);

                // Handle voice reminders BEFORE showing notification
                // Use database task as source of truth for reminder type if available
                String finalReminderType = (foundTask != null) ? foundTask.reminderType : reminderType;
                
                if ("voice".equals(finalReminderType)) {
                    Log.d(TAG, "Voice reminder detected for task " + taskId);
                    if (foundTask != null) {
                        speakVoiceReminder(context, foundTask);
                        return; // Don't show notification for voice reminders
                    }
                }

                // Show Notification (for notification and alarm reminders)
                Log.d(TAG, "Showing notification for task " + taskId + " with type: " + finalReminderType);

                Intent mainIntent = new Intent(context, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId, mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                String contentText = taskTitle != null ? taskTitle : "You have a reminder";
                if (taskDescription != null && !taskDescription.isEmpty()) {
                    contentText = taskTitle + ": " + taskDescription;
                }

                // Choose Channel based on type
                String channelId = "alarm".equals(finalReminderType) ? 
                        NotificationHelper.ALARM_CHANNEL_ID : NotificationHelper.CHANNEL_ID;
                
                Log.d(TAG, "Using channel: " + channelId + " for type: " + finalReminderType);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_alarm)
                        .setContentTitle("NextDO Reminder")
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                // Default notification behavior (only for non-alarms)
                if (!"alarm".equals(finalReminderType)) {
                    builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setDefaults(NotificationCompat.DEFAULT_ALL);
                }

                // For ALARM type: Use Full-Screen Intent (Standard Android 10+ way)
                if ("alarm".equals(finalReminderType)) {
                    Log.d(TAG, "Setting up full-screen alarm for task " + taskId);

                    // Launch AlarmActivity
                    Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
                    fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    // Pass necessary data to AlarmActivity
                    fullScreenIntent.putExtra("task_id", taskId);
                    fullScreenIntent.putExtra("task_title", taskTitle);
                    fullScreenIntent.putExtra("task_description", taskDescription);
                    fullScreenIntent.putExtra("alarm_id", intent.getIntExtra("alarm_id", 0));

                    PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, taskId + 10000,
                            fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    builder.setFullScreenIntent(fullScreenPendingIntent, true)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setOngoing(true) // Keeps notification until dismissed via Alarm UI
                            .setAutoCancel(false) // Alarms shouldn't auto-cancel
                            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                            .setVibrate(new long[] { 0, 1000, 500, 1000, 500, 1000 });
                    
                    Log.d(TAG, "Full-screen intent set for task " + taskId + " on channel " + channelId);
                }

                // Add Mark as Done Action
                Intent doneIntent = new Intent(context, MarkAsDoneReceiver.class);
                doneIntent.putExtra(EXTRA_TASK_ID, taskId);
                doneIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
                PendingIntent donePendingIntent = PendingIntent.getBroadcast(context, taskId + 30000, doneIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                builder.addAction(R.drawable.ic_check, "Mark as done", donePendingIntent);

                // Add Snooze Action
                int alarmId = intent.getIntExtra("alarm_id", 0);
                Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
                snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);
                snoozeIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
                snoozeIntent.putExtra("alarm_id", alarmId);
                snoozeIntent.putExtra("task_description", taskDescription);
                snoozeIntent.putExtra("reminder_type", finalReminderType);
                PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, taskId + 20000, snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                builder.addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                if (ActivityCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    
                    android.app.Notification notification = builder.build();
                    
                    // CRITICAL: Make the alarm sound loop until the user interacts with it
                    if ("alarm".equals(finalReminderType)) {
                        notification.flags |= android.app.Notification.FLAG_INSISTENT;
                    }
                    
                    notificationManager.notify(taskId, notification);
                    Log.d(TAG, "Notification displayed successfully for task " + taskId + " with flags: " + notification.flags);
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
