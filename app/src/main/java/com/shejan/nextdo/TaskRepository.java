package com.shejan.nextdo;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import java.util.List;

// DEFINITIVE FIX: Rewriting the repository to use separate insert and update methods.
// ADDITIONAL FIX: Added callback support for post-database operations
public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private final TaskDao taskDao;
    private final LiveData<List<Task>> allTasks;

    TaskRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        taskDao = db.taskDao();
        allTasks = taskDao.getAllTasks();
    }

    LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    void insert(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.insert(task);
        });
    }

    void insert(Task task, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Log.d(TAG, "Inserting task: " + task.title);
            // CRITICAL FIX: Get the generated ID and update the task object
            long newId = taskDao.insert(task);  // ← Now returns the generated ID
            task.id = (int) newId;  // ← Assign it back to the original object
            Log.d(TAG, "Insert complete for task: " + task.title + " (assigned id=" + newId + ")");
            if (onComplete != null) {
                onComplete.run();  // ← Now called with valid task.id!
            }
        });
    }

    void update(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.update(task);
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
        });
    }

    void delete(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.delete(task);
        });
    }

    LiveData<List<Task>> searchTasks(String query) {
        return taskDao.searchTasks(query);
    }
}
