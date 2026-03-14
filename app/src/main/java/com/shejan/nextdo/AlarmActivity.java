package com.shejan.nextdo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import android.speech.tts.TextToSpeech;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";
    private TextToSpeech tts;
    private Handler ttsHandler;
    private Runnable ttsRunnable;
    private int repeatCount = 0;
    private static final int MAX_REPEATS = 5;
    private static final long REPEAT_INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm);

        // Set status bar color to match background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.alarm_background_color));
        }

        // Get task details from intent
        String taskTitle = getIntent().getStringExtra("task_title");
        String taskDescription = getIntent().getStringExtra("task_description");
        int taskId = getIntent().getIntExtra("task_id", 0);
        String reminderType = getIntent().getStringExtra("reminder_type");

        // Setup UI
        TextView titleText = findViewById(R.id.alarm_title);
        TextView descText = findViewById(R.id.alarm_description);
        Button doneButton = findViewById(R.id.btn_done);
        Button snoozeButton = findViewById(R.id.btn_snooze);

        titleText.setText(taskTitle != null ? taskTitle : "Reminder");
        // Description is now hidden by default in XML as requested
        descText.setVisibility(android.view.View.GONE);

        // Handle Voice Reminder
        if ("voice".equals(reminderType)) {
            setupVoiceReminder(taskTitle, taskDescription);
        }

        // NOTE: Sound and vibration are managed by the notification system (FLAG_INSISTENT)
        // launched via ReminderBroadcastReceiver. cancelNotification() below stops it.

        // Done button (Mark as Done logic)
        doneButton.setOnClickListener(v -> {
            stopVoice();
            markTaskAsDone(taskId);
        });

        // Snooze button
        snoozeButton.setOnClickListener(v -> {
            stopVoice();
            // CRITICAL: Check if task is still valid (not deleted) before snoozing
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    Task task = db.taskDao().getTaskById(taskId);
                    if (task == null || task.isDeleted) {
                        runOnUiThread(() -> {
                            cancelNotification(taskId);
                            android.widget.Toast.makeText(this, "This reminder has been deleted",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }

                    // Proceed with snooze
                    runOnUiThread(() -> {
                        cancelNotification(taskId);

                        // Send broadcast to SnoozeReceiver
                        Intent snoozeIntent = new Intent(this, SnoozeReceiver.class);
                        snoozeIntent.putExtra("task_id", taskId);
                        snoozeIntent.putExtra("task_title", taskTitle);
                        snoozeIntent.putExtra("task_description", taskDescription);
                        snoozeIntent.putExtra("alarm_id", getIntent().getIntExtra("alarm_id", 0));
                        snoozeIntent.putExtra("reminder_type", reminderType != null ? reminderType : "alarm");
                        sendBroadcast(snoozeIntent);

                        finish();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error checking task status: " + e.getMessage());
                    runOnUiThread(() -> {
                        cancelNotification(taskId);
                        finish();
                    });
                }
            }).start();
        });

        // Handle back button press - prevent dismissing alarm
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent back button from dismissing alarm
                // User must explicitly dismiss or snooze
            }
        });
    }

    private void setupVoiceReminder(String title, String description) {
        final String textToSpeak = "Reminder: " + title + (description != null && !description.isEmpty() ? ". " + description : "");
        
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                setMaleVoice(tts);
                
                ttsHandler = new Handler(Looper.getMainLooper());
                ttsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (repeatCount < MAX_REPEATS) {
                            Log.d(TAG, "Speaking voice reminder (Attempt " + (repeatCount + 1) + "): " + textToSpeak);
                            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "reminder_" + repeatCount);
                            repeatCount++;
                            ttsHandler.postDelayed(this, REPEAT_INTERVAL);
                        }
                    }
                };
                ttsHandler.post(ttsRunnable);
            } else {
                Log.e(TAG, "TTS Initialization failed");
            }
        });
    }

    private void setMaleVoice(TextToSpeech tts) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                java.util.Set<android.speech.tts.Voice> voices = tts.getVoices();
                if (voices != null) {
                    for (android.speech.tts.Voice voice : voices) {
                        if (voice.getLocale().equals(Locale.US)) {
                            String voiceName = voice.getName().toLowerCase();
                            if (voiceName.contains("male") && !voiceName.contains("female")) {
                                tts.setVoice(voice);
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

    private void stopVoice() {
        if (ttsHandler != null && ttsRunnable != null) {
            ttsHandler.removeCallbacks(ttsRunnable);
        }
        if (tts != null) {
            tts.stop();
        }
    }

    private void markTaskAsDone(int taskId) {
        cancelNotification(taskId);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                TaskDao taskDao = db.taskDao();
                Task task = taskDao.getTaskById(taskId);

                if (task != null) {
                    // Mark as completed
                    task.isCompleted = true;
                    task.completedTimestamp = System.currentTimeMillis();
                    taskDao.update(task);

                    // Cancel any scheduled alarms for this task
                    AlarmScheduler alarmScheduler = new AlarmScheduler(this);
                    alarmScheduler.cancel(task);

                    // Notify widgets/UI
                    UpcomingTasksWidgetProvider.sendRefreshBroadcast(this);

                    Log.d(TAG, "Task " + taskId + " marked as done from Alarm Screen");
                    
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Task marked as done", android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(this::finish);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking task as done: " + e.getMessage());
                runOnUiThread(this::finish);
            }
        });
    }

    private void cancelNotification(int taskId) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(taskId);
                Log.d(TAG, "Alarm notification " + taskId + " cancelled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        stopVoice();
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }
}
