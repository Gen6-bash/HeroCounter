package com.herocounter.app;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Task.class, CountEntry.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TaskDao taskDao();
    public abstract CountEntryDao countEntryDao();

    // v1 → v2: add goal columns
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN dailyGoal INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN weeklyGoal INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN monthlyGoal INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN yearlyGoal INTEGER NOT NULL DEFAULT 0");
        }
    };

    // v2 → v3: add reminder columns
    // reminderDays is TEXT nullable (no NOT NULL) to match the @Nullable annotation on Task
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT 9");
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinute INTEGER NOT NULL DEFAULT 0");
            // TEXT column with no NOT NULL constraint = nullable, matching @Nullable in Task.java
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDays TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "herocounter.db"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
