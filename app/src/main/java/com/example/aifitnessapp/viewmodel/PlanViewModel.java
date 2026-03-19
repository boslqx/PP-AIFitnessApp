package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.repository.PlanRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanViewModel extends AndroidViewModel {

    private FitAIDatabase db;
    private PlanRepository planRepo;

    public LiveData<UserPreferences>      currentUser;
    public LiveData<List<PlannedWorkout>> weekPlan;
    public MutableLiveData<Boolean>       planReady   = new MutableLiveData<>(false);

    // Map of plannedWorkoutId → WorkoutLog (so we know which days are logged)
    public MutableLiveData<Map<Integer, WorkoutLog>> logMap = new MutableLiveData<>(new HashMap<>());

    // Summary: "Week 1 · 4 workouts planned"
    public MutableLiveData<String> planSummary = new MutableLiveData<>("");

    public PlanViewModel(@NonNull Application application) {
        super(application);
        db       = FitAIDatabase.getInstance(application);
        planRepo = new PlanRepository(application);
        currentUser = db.userPreferencesDao().getCurrentUser();
    }

    public void loadPlan(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // Ensure plan exists
            if (!planRepo.planExistsForThisWeek(userId)) {
                planRepo.generateNextWeekPlan(userId);
            }
            planReady.postValue(true);
        });
    }

    public void initWeekPlan(int userId) {
        String weekStart = PlanEngine.getCurrentWeekStart();
        weekPlan = planRepo.getWeekPlan(userId, weekStart);
    }

    /*
     * Loads all logs for this week and builds a map:
     * plannedWorkoutId → WorkoutLog
     * Used by the UI to show ✅ Logged / ✏️ Modified / ⏭️ Skipped on each day
     */
    public void loadLogMap(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            String weekStart = PlanEngine.getCurrentWeekStart();
            List<WorkoutLog> logs = db.workoutLogDao().getLogsSince(userId, weekStart);

            Map<Integer, WorkoutLog> map = new HashMap<>();
            for (WorkoutLog log : logs) {
                map.put(log.plannedWorkoutId, log);
            }
            logMap.postValue(map);
        });
    }

    public void buildSummary(List<PlannedWorkout> plans, int weekNum) {
        if (plans == null) return;
        int workouts = 0;
        for (PlannedWorkout p : plans) if (!p.isRestDay) workouts++;
        planSummary.postValue("Week " + weekNum + "  ·  " + workouts + " workouts planned");
    }

    public int getCurrentWeekNum(int userId) {
        return db.plannedWorkoutDao().getLatestPlanWeek(userId);
    }
}