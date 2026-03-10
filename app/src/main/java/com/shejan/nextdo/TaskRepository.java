package com.shejan.nextdo;

import android.app.Application;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import java.util.List;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private final TaskDao taskDao;

    private final Application application;
    private final AlarmScheduler alarmScheduler;

    TaskRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        taskDao = db.taskDao();
        this.alarmScheduler = new AlarmScheduler(application);
    }

    LiveData<List<Task>> getActiveTasks() {
        return taskDao.getActiveTasks();
    }

    LiveData<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks();
    }

    LiveData<List<Task>> getAllNonDeletedTasks() {
        return taskDao.getAllNonDeletedTasks();
    }

    void insert(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.insert(task);
            UpcomingTasksWidgetProvider.sendRefreshBroadcast(application);
        });
    }

    void insert(Task task, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Log.d(TAG, "Inserting task: " + task.title);
            long newId = taskDao.insert(task);
            task.id = (int) newId;
            Log.d(TAG, "Insert complete for task: " + task.title + " (assigned id=" + newId + ")");
            if (onComplete != null) {
                onComplete.run();
            }
            UpcomingTasksWidgetProvider.sendRefreshBroadcast(application);
        });
    }

    void update(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.update(task);
            UpcomingTasksWidgetProvider.sendRefreshBroadcast(application);
        });
    }

    void update(Task task, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Log.d(TAG, "Updating task: " + task.title);
            taskDao.update(task);
            Log.d(TAG, "Update complete for task: " + task.title);
            if (onComplete != null) {
                onComplete.run();
            }
            UpcomingTasksWidgetProvider.sendRefreshBroadcast(application);
        });
    }

    void delete(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Cancel alarm and dismiss notification before deleting
            alarmScheduler.cancel(task);
            dismissNotification(task.id);

            taskDao.delete(task);
            UpcomingTasksWidgetProvider.sendRefreshBroadcast(application);
        });
    }

    public LiveData<List<Task>> getDeletedTasks() {
        return taskDao.getDeletedTasks();
    }

    public void deleteOldTasks(long threshold) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.deleteOldTasks(threshold));
    }

    public void deleteAllDeletedTasks() {
        AppDatabase.databaseWriteExecutor.execute(taskDao::deleteAllDeletedTasks);
    }

    public void deletePermanently(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Cancel alarm and dismiss notification before permanent deletion
            alarmScheduler.cancel(task);
            dismissNotification(task.id);

            taskDao.delete(task);
        });
    }

    public void softDelete(Task task) {
        // Cancel alarm and dismiss notification when soft deleting
        alarmScheduler.cancel(task);
        dismissNotification(task.id);

        task.isDeleted = true;
        task.deletedTimestamp = System.currentTimeMillis();
        update(task);
    }

    public void restore(Task task) {
        task.isDeleted = false;
        task.deletedTimestamp = 0;
        update(task);
    }

    public void deleteOldCompletedTasks(long threshold) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.deleteOldCompletedTasks(threshold));
    }

    /**
     * Dismisses the notification for a given task ID
     */
    private void dismissNotification(int taskId) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(application);
            notificationManager.cancel(taskId);
            Log.d(TAG, "Notification dismissed for task " + taskId);
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing notification: " + e.getMessage());
        }
    }
}
