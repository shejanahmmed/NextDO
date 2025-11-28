package com.shejan.nextdo;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
    }

    public LiveData<List<Task>> getActiveTasks() {
        return repository.getActiveTasks();
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return repository.getCompletedTasks();
    }

    public void insert(Task task) {
        repository.insert(task);
    }

    public void insert(Task task, Runnable onComplete) {
        repository.insert(task, onComplete);
    }

    public void update(Task task) {
        repository.update(task);
    }

    public void update(Task task, Runnable onComplete) {
        repository.update(task, onComplete);
    }

    public void delete(Task task) {
        repository.delete(task);
    }

    public LiveData<List<Task>> getDeletedTasks() {
        return repository.getDeletedTasks();
    }

    public void deleteOldTasks(long threshold) {
        repository.deleteOldTasks(threshold);
    }

    public void deleteAllDeletedTasks() {
        repository.deleteAllDeletedTasks();
    }

    public void deletePermanently(Task task) {
        repository.deletePermanently(task);
    }

    public void softDelete(Task task) {
        repository.softDelete(task);
    }

    public void restore(Task task) {
        repository.restore(task);
    }

    public void deleteOldCompletedTasks(long threshold) {
        repository.deleteOldCompletedTasks(threshold);
    }
}
