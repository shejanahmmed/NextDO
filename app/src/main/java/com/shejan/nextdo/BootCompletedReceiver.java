package com.shejan.nextdo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all active alarms after device reboot
            // This ensures reminders work after device restart
            Log.d(TAG, "Device boot completed, rescheduling all alarms");
            rescheduleAllAlarms(context);
        }
    }

    private void rescheduleAllAlarms(Context context) {
        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            TaskDao taskDao = db.taskDao();
            AlarmScheduler alarmScheduler = new AlarmScheduler(context);
            
            // Run on background thread to avoid blocking boot process
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    // Get all tasks with reminders
                    java.util.List<Task> allTasks = taskDao.getAllTasksSync();
                    long currentTime = System.currentTimeMillis();
                    
                    Log.d(TAG, "Found " + allTasks.size() + " total tasks");
                    int rescheduledCount = 0;
                    
                    for (Task task : allTasks) {
                        // CRITICAL BUG FIX 3: Recovery for repeating tasks
                        // If task is repeating and time has passed, calculate next occurrence
                        if (!task.isCompleted && task.alarmId != 0 && !android.text.TextUtils.isEmpty(task.repeat)) {
                             if (task.reminderTime < currentTime) {
                                 long nextTime = AlarmScheduler.getNextOccurrence(currentTime, task.reminderTime, task.repeat);
                                 if (nextTime > currentTime) {
                                     Log.d(TAG, "Boot Recovery: Missed repeating task " + task.id + ", rescheduling for " + nextTime);
                                     task.reminderTime = nextTime;
                                     taskDao.update(task);
                                     alarmScheduler.schedule(task);
                                     rescheduledCount++;
                                 }
                             } else {
                                 // Future repeating task
                                 Log.d(TAG, "Rescheduling future repeating task " + task.id);
                                 alarmScheduler.schedule(task);
                                 rescheduledCount++;
                             }
                        } else if (task.reminderTime > currentTime && !task.isCompleted && task.alarmId != 0) {
                            // Non-repeating future task
                            Log.d(TAG, "Rescheduling alarm for task " + task.id);
                            alarmScheduler.schedule(task);
                            rescheduledCount++;
                        }
                    }
                    Log.d(TAG, "Rescheduled " + rescheduledCount + " alarms after boot");
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling alarms: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in rescheduleAllAlarms: " + e.getMessage(), e);
        }
    }
}