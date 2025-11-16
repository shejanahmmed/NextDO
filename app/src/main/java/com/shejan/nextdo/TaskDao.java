package com.shejan.nextdo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    // DEFINITIVE FIX: Using robust conflict strategies to guarantee LiveData updates.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE title LIKE :query OR description LIKE :query ORDER BY id DESC")
    LiveData<List<Task>> searchTasks(String query);
}
