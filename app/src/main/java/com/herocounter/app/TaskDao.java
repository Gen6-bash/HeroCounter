package com.herocounter.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, createdAt ASC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskById(int id);

    @Query("SELECT MAX(sortOrder) FROM tasks")
    int getMaxSortOrder();

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    void updateSortOrder(int id, int sortOrder);
}
