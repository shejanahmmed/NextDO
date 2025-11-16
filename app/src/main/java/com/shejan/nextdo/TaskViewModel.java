package com.shejan.nextdo;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

// DEFINITIVE FIX: Rewriting the ViewModel to use separate insert and update methods.
public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final LiveData<List<Task>> allTasks;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        allTasks = repository.getAllTasks();
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    public void insert(Task task) {
        repository.insert(task);
    }

    public void update(Task task) {
        repository.update(task);
    }

    public void delete(Task task) {
        repository.delete(task);
    }
}
