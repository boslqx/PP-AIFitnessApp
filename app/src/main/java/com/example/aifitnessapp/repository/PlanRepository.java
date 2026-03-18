package com.example.aifitnessapp.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import java.util.List;

public class PlanRepository {

    private FitAIDatabase db;

    public PlanRepository(Application application) {
        db = FitAIDatabase.getInstance(application);
    }

    // ── Called after onboarding completes ──────────────────
    // Generates week 1 plan with no prior logs
    public void generateFirstPlan(int userId) {
        UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
        if (prefs == null) return;

        String weekStart = PlanEngine.getCurrentWeekStart();
        int planWeek = 1;

        List<PlannedWorkout> plan = PlanEngine.generateWeekPlan(
                prefs, null, planWeek, weekStart);

        db.plannedWorkoutDao().insertAll(plan);
    }

    // ── Called every Monday (or on demand) ─────────────────
    // Generates next week's plan based on last week's logs
    public void generateNextWeekPlan(int userId) {
        UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
        if (prefs == null) return;

        // Get last week's logs for adaptation
        String weekStart = PlanEngine.getCurrentWeekStart();
        List<WorkoutLog> recentLogs = db.workoutLogDao()
                .getLogsSince(userId, weekStart);

        // Determine next week number
        int lastWeek = db.plannedWorkoutDao().getLatestPlanWeek(userId);
        int nextWeek = lastWeek + 1;

        // Next week's Monday
        String[] parts = weekStart.split("-");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        try {
            cal.set(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]));
        } catch (Exception e) { /* fallback to today */ }
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7);
        String nextWeekStart = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.getTime());

        List<PlannedWorkout> plan = PlanEngine.generateWeekPlan(
                prefs, recentLogs, nextWeek, nextWeekStart);

        db.plannedWorkoutDao().insertAll(plan);
    }

    // ── LiveData queries for UI ─────────────────────────────

    public LiveData<List<PlannedWorkout>> getWeekPlan(int userId, String weekStart) {
        return db.plannedWorkoutDao().getWeekPlan(userId, weekStart);
    }

    public LiveData<PlannedWorkout> getTodayPlan(int userId) {
        String weekStart = PlanEngine.getCurrentWeekStart();
        String today     = PlanEngine.getTodayDayOfWeek();
        return db.plannedWorkoutDao().getTodayPlan(userId, weekStart, today);
    }

    public PlannedWorkout getTodayPlanSync(int userId) {
        String weekStart = PlanEngine.getCurrentWeekStart();
        String today     = PlanEngine.getTodayDayOfWeek();
        return db.plannedWorkoutDao().getTodayPlanSync(userId, weekStart, today);
    }

    public List<PlannedWorkout> getWeekPlanSync(int userId, String weekStart) {
        return db.plannedWorkoutDao().getWeekPlanSync(userId, weekStart);
    }

    // ── Check if plan exists for this week ─────────────────
    public boolean planExistsForThisWeek(int userId) {
        String weekStart = PlanEngine.getCurrentWeekStart();
        List<PlannedWorkout> existing = db.plannedWorkoutDao()
                .getWeekPlanSync(userId, weekStart);
        return existing != null && !existing.isEmpty();
    }
}