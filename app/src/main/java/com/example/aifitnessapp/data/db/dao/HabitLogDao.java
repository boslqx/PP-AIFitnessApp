package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.HabitLog;
import java.util.List;

@Dao
public interface HabitLogDao {

    @Insert
    void insert(HabitLog habitLog);

    @Update
    void update(HabitLog habitLog);

    @Delete
    void delete(HabitLog habitLog);

    @Query("SELECT * FROM habit_log WHERE userId = :userId AND date = :date")
    LiveData<List<HabitLog>> getHabitsByDate(int userId, String date);

    @Query("SELECT * FROM habit_log WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    LiveData<List<HabitLog>> getLast7DaysHabits(int userId);

    // For consistency engine: how many habits completed today
    @Query("SELECT COUNT(*) FROM habit_log WHERE userId = :userId AND date = :date AND completed = 1")
    int countCompletedHabitsOnDate(int userId, String date);

    @Query("SELECT COUNT(*) FROM habit_log WHERE userId = :userId AND date = :date")
    int countTotalHabitsOnDate(int userId, String date);
}