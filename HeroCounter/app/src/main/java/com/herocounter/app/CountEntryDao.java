package com.herocounter.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CountEntryDao {

    @Insert
    void insert(CountEntry entry);

    // Total for a specific task on a specific day
    @Query("SELECT COALESCE(SUM(delta), 0) FROM count_entries WHERE taskId = :taskId AND dateDay = :day")
    int getTotalForDay(int taskId, String day);

    // Total for a specific task in a specific week
    @Query("SELECT COALESCE(SUM(delta), 0) FROM count_entries WHERE taskId = :taskId AND dateWeek = :week")
    int getTotalForWeek(int taskId, String week);

    // Total for a specific task in a specific month
    @Query("SELECT COALESCE(SUM(delta), 0) FROM count_entries WHERE taskId = :taskId AND dateMonth = :month")
    int getTotalForMonth(int taskId, String month);

    // Total for a specific task in a specific year
    @Query("SELECT COALESCE(SUM(delta), 0) FROM count_entries WHERE taskId = :taskId AND dateYear = :year")
    int getTotalForYear(int taskId, String year);

    // All entries for a task grouped by day (for bar chart)
    @Query("SELECT dateDay as period, SUM(delta) as total FROM count_entries WHERE taskId = :taskId GROUP BY dateDay ORDER BY dateDay ASC")
    List<PeriodTotal> getDailyTotals(int taskId);

    // All entries for a task grouped by week
    @Query("SELECT dateWeek as period, SUM(delta) as total FROM count_entries WHERE taskId = :taskId GROUP BY dateWeek ORDER BY dateWeek ASC")
    List<PeriodTotal> getWeeklyTotals(int taskId);

    // All entries for a task grouped by month
    @Query("SELECT dateMonth as period, SUM(delta) as total FROM count_entries WHERE taskId = :taskId GROUP BY dateMonth ORDER BY dateMonth ASC")
    List<PeriodTotal> getMonthlyTotals(int taskId);

    // All entries for a task grouped by year
    @Query("SELECT dateYear as period, SUM(delta) as total FROM count_entries WHERE taskId = :taskId GROUP BY dateYear ORDER BY dateYear ASC")
    List<PeriodTotal> getYearlyTotals(int taskId);


    // Check if an entry already exists at this exact timestamp for this task (for import dedup)
    @Query("SELECT COUNT(*) FROM count_entries WHERE taskId = :taskId AND timestamp = :timestamp AND delta = :delta")
    int countDuplicates(int taskId, long timestamp, int delta);

    // All raw entries for CSV export (single task)
    @Query("SELECT * FROM count_entries WHERE taskId = :taskId ORDER BY timestamp ASC")
    List<CountEntry> getAllEntriesForTask(int taskId);

    // All raw entries for CSV export (all tasks)
    @Query("SELECT * FROM count_entries ORDER BY taskId ASC, timestamp ASC")
    List<CountEntry> getAllEntries();

    // Delete all entries for a task (used when task is deleted)
    @Query("DELETE FROM count_entries WHERE taskId = :taskId")
    void deleteAllForTask(int taskId);
}
