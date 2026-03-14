package com.shejan.nextdo;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

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

        // Setup UI
        TextView titleText = findViewById(R.id.alarm_title);
        TextView descText = findViewById(R.id.alarm_description);
        Button dismissButton = findViewById(R.id.btn_dismiss);
        Button snoozeButton = findViewById(R.id.btn_snooze);

        titleText.setText(taskTitle != null ? taskTitle : "Reminder");
        // Description is now hidden by default in XML as requested
        descText.setVisibility(android.view.View.GONE);

        // Start alarm sound and vibration
        startAlarmSound();
        startVibration();

        // Dismiss button
        dismissButton.setOnClickListener(v -> {
            stopAlarmSound();
            stopVibration();
            finish();
        });

        // Snooze button
        snoozeButton.setOnClickListener(v -> {
            // CRITICAL: Check if task is still valid (not deleted) before snoozing
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    Task task = db.taskDao().getTaskById(taskId);
                    if (task == null || task.isDeleted) {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(this, "This reminder has been deleted",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            stopAlarmSound();
                            stopVibration();
                            finish();
                        });
                        return;
                    }

                    // Proceed with snooze
                    runOnUiThread(() -> {
                        stopAlarmSound();
                        stopVibration();

                        // Send broadcast to SnoozeReceiver
                        Intent snoozeIntent = new Intent(this, SnoozeReceiver.class);
                        snoozeIntent.putExtra("task_id", taskId);
                        snoozeIntent.putExtra("task_title", taskTitle);
                        snoozeIntent.putExtra("task_description", taskDescription);
                        snoozeIntent.putExtra("alarm_id", getIntent().getIntExtra("alarm_id", 0));
                        snoozeIntent.putExtra("reminder_type", "alarm"); // CRITICAL BUG FIX 1: Always "alarm" here
                        sendBroadcast(snoozeIntent);

                        finish();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error checking task status: " + e.getMessage());
                    runOnUiThread(() -> {
                        stopAlarmSound();
                        stopVibration();
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

    private void startAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mediaPlayer.setAudioAttributes(attributes);
            } else {
                mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start alarm sound", e);
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = { 0, 1000, 500, 1000, 500, 1000 };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
        stopVibration();
    }

}
