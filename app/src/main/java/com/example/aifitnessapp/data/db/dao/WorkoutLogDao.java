package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.WorkoutLog;
import java.util.List;

@Dao
public interface WorkoutLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkoutLog log);

    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<WorkoutLog>> getAllLogs(int userId);

    @Query("SELECT * FROM workout_logs WHERE userId = :userId AND date >= :since ORDER BY date ASC")
    List<WorkoutLog> getLogsSince(int userId, String since);

    @Query("SELECT * FROM workout_logs WHERE userId = :userId AND date = :date LIMIT 1")
    WorkoutLog getLogForDate(int userId, String date);

    @Query("SELECT * FROM workout_logs WHERE plannedWorkoutId = :plannedId LIMIT 1")
    WorkoutLog getLogForPlannedWorkout(int plannedId);

    // For adaptation: how many completed vs skipped in last N logs
    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    List<WorkoutLog> getRecentLogs(int userId, int limit);

    @Query("SELECT COUNT(*) FROM workout_logs WHERE userId = :userId AND completionStatus = 'COMPLETED' AND date >= :since")
    int countCompletedSince(int userId, String since);

    @Query("SELECT COUNT(*) FROM workout_logs WHERE userId = :userId AND completionStatus = 'SKIPPED' AND date >= :since")
    int countSkippedSince(int userId, String since);

    @Query("DELETE FROM workout_logs")
    void deleteAll();
}