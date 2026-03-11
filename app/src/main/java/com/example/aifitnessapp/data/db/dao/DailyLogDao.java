package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.DailyLog;
import java.util.List;

@Dao
public interface DailyLogDao {

    @Insert
    void insert(DailyLog log);

    @Update
    void update(DailyLog log);

    @Query("SELECT * FROM daily_log WHERE userId = :userId ORDER BY date DESC LIMIT 30")
    LiveData<List<DailyLog>> getLast30Days(int userId);

    @Query("SELECT * FROM daily_log WHERE userId = :userId AND date = :date LIMIT 1")
    LiveData<DailyLog> getLogByDate(int userId, String date);

    // Returns plain List (not LiveData) for background ML processing
    @Query("SELECT * FROM daily_log WHERE userId = :userId ORDER BY date ASC")
    List<DailyLog> getAllLogsSync(int userId);

    // For trend calculation
    @Query("SELECT AVG(sleepHours) FROM daily_log WHERE userId = :userId AND date >= :fromDate")
    float getAvgSleepSince(int userId, String fromDate);

    @Query("SELECT AVG(caloriesConsumed) FROM daily_log WHERE userId = :userId AND date >= :fromDate")
    float getAvgCaloriesSince(int userId, String fromDate);
}
