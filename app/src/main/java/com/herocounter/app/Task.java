package com.herocounter.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    public long createdAt;

    // Goals (0 = not set)
    public int dailyGoal;
    public int weeklyGoal;
    public int monthlyGoal;
    public int yearlyGoal;

    // Reminder fields — nullable to satisfy Room migration validation
    public boolean reminderEnabled;
    public int reminderHour;
    public int reminderMinute;
    @Nullable
    public String reminderDays;

    // Display order in the task list
    public int sortOrder;

    public Task(@NonNull String name, long createdAt,
                int dailyGoal, int weeklyGoal, int monthlyGoal, int yearlyGoal) {
        this.name            = name;
        this.createdAt       = createdAt;
        this.dailyGoal       = dailyGoal;
        this.weeklyGoal      = weeklyGoal;
        this.monthlyGoal     = monthlyGoal;
        this.yearlyGoal      = yearlyGoal;
        this.reminderEnabled = false;
        this.reminderHour    = 9;
        this.reminderMinute  = 0;
        this.reminderDays    = "2,3,4,5,6,7,1";
        this.sortOrder       = 0;
    }
}
