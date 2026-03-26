package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import java.util.List;

@Dao
public interface PlannedWorkoutDao {

    // ── INSERT ────────────────────────────────────────────────
    // Used when generating a new week's plan from scratch.
    // REPLACE strategy: if a row with the same primary key exists,
    // delete it and insert the new one. Fine for bulk generation.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PlannedWorkout> workouts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlannedWorkout workout);

    // ── UPDATE ────────────────────────────────────────────────
    // Room matches the row by the @PrimaryKey field (id).
    // Only the columns that changed are rewritten — safe and precise.
    // We use this for ALL user edits (swap, change activity, cancel).
    @Update
    void update(PlannedWorkout workout);

    // Convenience: update two workouts in one call.
    // Used for day-swap so both sides change atomically.
    // "Atomically" means: both succeed or both fail — no half-swaps.
    @Update
    void updateAll(List<PlannedWorkout> workouts);

    // ── SELECT — LiveData (for UI observation) ────────────────
    // LiveData automatically notifies observers when the data changes.
    // This means after we call update(), the UI refreshes itself —
    // we don't need to manually reload anything.
    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart ORDER BY id ASC")
    LiveData<List<PlannedWorkout>> getWeekPlan(int userId, String weekStart);

    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart AND dayOfWeek = :day LIMIT 1")
    LiveData<PlannedWorkout> getTodayPlan(int userId, String weekStart, String day);

    // ── SELECT — Synchronous (for background thread work) ─────
    // These do NOT return LiveData — they block until the query finishes.
    // MUST be called on a background thread (never on the main/UI thread).
    // We use these inside Repository methods that run on AppExecutors.diskIO()
    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart ORDER BY id ASC")
    List<PlannedWorkout> getWeekPlanSync(int userId, String weekStart);

    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND weekStartDate = :weekStart AND dayOfWeek = :day LIMIT 1")
    PlannedWorkout getTodayPlanSync(int userId, String weekStart, String day);

    // Fetch a single workout by its primary key.
    // Used when we know exactly which row we want to edit.
    @Query("SELECT * FROM planned_workouts WHERE id = :id LIMIT 1")
    PlannedWorkout getByIdSync(int id);

    // ── META QUERIES ──────────────────────────────────────────
    @Query("SELECT MAX(planWeek) FROM planned_workouts WHERE userId = :userId")
    int getLatestPlanWeek(int userId);

    @Query("DELETE FROM planned_workouts WHERE userId = :userId AND planWeek = :week")
    void deletePlanForWeek(int userId, int week);

    @Query("DELETE FROM planned_workouts")
    void deleteAll();
}