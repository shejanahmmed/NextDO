package com.shejan.nextdo;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

// DEFINITIVE FIX: Rewriting the repository to use separate insert and update methods.
public class TaskRepository {
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

    void update(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.update(task);
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
