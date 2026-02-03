package com.shejan.nextdo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// DEFINITIVE FIX: Upgrading the database to version 2.
@Database(entities = { Task.class }, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN alarmId INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN deletedTimestamp INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN completedTimestamp INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // This migration previously tried to add the column, but schema mismatch
            // occurred.
            // We'll keep it for legacy support, but the real fix is ensuring non-null
            // constraint matches in v6.
            try {
                database.execSQL("ALTER TABLE tasks ADD COLUMN reminderType TEXT DEFAULT 'notification'");
            } catch (Exception e) {
                // Column might already exist from failed previous attempt, ignore
            }
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create a new table with the correct schema
            database.execSQL("CREATE TABLE IF NOT EXISTS `tasks_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`priority` TEXT, " +
                    "`reminderTime` INTEGER NOT NULL, " +
                    "`repeat` TEXT, " +
                    "`reminderType` TEXT NOT NULL DEFAULT 'notification', " +
                    "`isCompleted` INTEGER NOT NULL, " +
                    "`alarmId` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`deletedTimestamp` INTEGER NOT NULL, " +
                    "`completedTimestamp` INTEGER NOT NULL)");

            // Copy data from old table to new table
            database.execSQL(
                    "INSERT INTO tasks_new (id, title, description, priority, reminderTime, repeat, reminderType, isCompleted, alarmId, isDeleted, deletedTimestamp, completedTimestamp) "
                            +
                            "SELECT id, title, description, priority, reminderTime, repeat, IFNULL(reminderType, 'notification'), isCompleted, alarmId, isDeleted, deletedTimestamp, completedTimestamp FROM tasks");

            // Drop old table
            database.execSQL("DROP TABLE tasks");

            // Rename new table to old table name
            database.execSQL("ALTER TABLE tasks_new RENAME TO tasks");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "task_database")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
