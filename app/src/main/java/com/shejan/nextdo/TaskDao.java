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

    // DEFINITIVE FIX: Using robust conflict strategies to guarantee LiveData
    // updates.
    // CRITICAL FIX: Return the generated ID so we can update the original task
    // object
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Task task); // ← Changed from void to long - returns generated ID

    @Update
    void update(Task task);

    @Delete
    void delete(Task task); // This is now "Delete Permanently" in the context of Recycle Bin, or used for
                            // soft delete if we update the object first.
                            // Actually, for soft delete we use @Update. This @Delete is for permanent
                            // removal.

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, id DESC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    List<Task> getAllTasksSync();

    @Query("SELECT * FROM tasks WHERE (title LIKE :query OR description LIKE :query) AND isDeleted = 0 ORDER BY id DESC")
    LiveData<List<Task>> searchTasks(String query);

    // Recycle Bin Queries
    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    LiveData<List<Task>> getDeletedTasks();

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedTimestamp < :threshold")
    void deleteOldTasks(long threshold);

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    void deleteAllDeletedTasks();
}
