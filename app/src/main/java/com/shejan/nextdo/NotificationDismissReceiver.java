package com.shejan.nextdo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class NotificationDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        boolean persistentEnabled = prefs.getBoolean("persistent_notifications", false);
        
        if (!persistentEnabled) return;
        
        String taskTitle = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE);
        String taskDescription = intent.getStringExtra("task_description");
        int taskId = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, 0);
        
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            Intent reminderIntent = new Intent(context, ReminderBroadcastReceiver.class);
            reminderIntent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, taskTitle);
            reminderIntent.putExtra("task_description", taskDescription);
            reminderIntent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, taskId);
            
            ReminderBroadcastReceiver receiver = new ReminderBroadcastReceiver();
            receiver.onReceive(context, reminderIntent);
        }, 5000);
    }
}