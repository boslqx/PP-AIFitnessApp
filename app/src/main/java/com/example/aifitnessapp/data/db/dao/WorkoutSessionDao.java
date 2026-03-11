package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.WorkoutSession;
import java.util.List;

@Dao
public interface WorkoutSessionDao {

    @Insert
    void insert(WorkoutSession session);

    @Update
    void update(WorkoutSession session);

    @Delete
    void delete(WorkoutSession session);

    @Query("SELECT * FROM workout_session WHERE userId = :userId AND date = :date")
    LiveData<List<WorkoutSession>> getSessionsByDate(int userId, String date);

    @Query("SELECT * FROM workout_session WHERE userId = :userId ORDER BY date DESC LIMIT 30")
    LiveData<List<WorkoutSession>> getLast30Sessions(int userId);

    // Plain List (no LiveData) for AI/ML background processing
    @Query("SELECT * FROM workout_session WHERE userId = :userId ORDER BY date ASC")
    List<WorkoutSession> getAllSessionsSync(int userId);

    // For adaptive plan engine: count completed vs planned in a date range
    @Query("SELECT COUNT(*) FROM workout_session WHERE userId = :userId AND date >= :fromDate AND completed = 1")
    int countCompletedSince(int userId, String fromDate);

    @Query("SELECT COUNT(*) FROM workout_session WHERE userId = :userId AND date >= :fromDate AND wasPlanned = 1")
    int countPlannedSince(int userId, String fromDate);
}
