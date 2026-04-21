package com.herocounter.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "count_entries",
    foreignKeys = @ForeignKey(
        entity = Task.class,
        parentColumns = "id",
        childColumns = "taskId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("taskId")}
)
public class CountEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int taskId;

    // Delta: positive for add, negative for subtract
    public int delta;

    // Timestamp in milliseconds
    public long timestamp;

    // Convenience date fields for fast queries (YYYY-MM-DD, YYYY-Www, YYYY-MM, YYYY)
    public String dateDay;   // e.g. "2024-03-15"
    public String dateWeek;  // e.g. "2024-W11"
    public String dateMonth; // e.g. "2024-03"
    public String dateYear;  // e.g. "2024"

    public CountEntry(int taskId, int delta, long timestamp,
                      String dateDay, String dateWeek, String dateMonth, String dateYear) {
        this.taskId = taskId;
        this.delta = delta;
        this.timestamp = timestamp;
        this.dateDay = dateDay;
        this.dateWeek = dateWeek;
        this.dateMonth = dateMonth;
        this.dateYear = dateYear;
    }
}
