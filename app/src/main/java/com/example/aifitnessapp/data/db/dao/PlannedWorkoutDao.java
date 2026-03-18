package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import java.util.List;

@Dao
public interface PlannedWorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PlannedWorkout> workouts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlannedWorkout workout);

    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart ORDER BY id ASC")
    LiveData<List<PlannedWorkout>> getWeekPlan(int userId, String weekStart);

    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart ORDER BY id ASC")
    List<PlannedWorkout> getWeekPlanSync(int userId, String weekStart);

    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart AND dayOfWeek = :day LIMIT 1")
    LiveData<PlannedWorkout> getTodayPlan(int userId, String weekStart, String day);

    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart AND dayOfWeek = :day LIMIT 1")
    PlannedWorkout getTodayPlanSync(int userId, String weekStart, String day);

    @Query("SELECT MAX(planWeek) FROM planned_workouts WHERE userId = :userId")
    int getLatestPlanWeek(int userId);

    @Query("DELETE FROM planned_workouts WHERE userId = :userId AND planWeek = :week")
    void deletePlanForWeek(int userId, int week);
}