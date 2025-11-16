package com.shejan.nextdo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title = "";
    public String description = "";
    public String priority = "";
    public long reminderTime = 0;
    public String repeat = "";
    public boolean isCompleted = false;
    public int alarmId = 0; // DEFINITIVE FIX: Add a dedicated, unique ID for alarms.
}
