package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.repository.PlanRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.List;
import androidx.lifecycle.Transformations;

public class HomeViewModel extends AndroidViewModel {

    private FitAIDatabase db;
    private PlanRepository planRepo;

    public LiveData<UserPreferences>       currentUser;
    public LiveData<PlannedWorkout>        todayPlan;
    public LiveData<List<PlannedWorkout>>  weekPlan;
    public MutableLiveData<String>         coachMessage = new MutableLiveData<>("");
    public MutableLiveData<Boolean>        planReady    = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        db       = FitAIDatabase.getInstance(application);
        planRepo = new PlanRepository(application);
        currentUser = db.userPreferencesDao().getCurrentUser();
    }

    // Called once we have userId from currentUser observer
    public void initPlan(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            // If no plan exists for this week, generate one
            if (!planRepo.planExistsForThisWeek(userId)) {
                planRepo.generateNextWeekPlan(userId);
            }

            planReady.postValue(true);
        });
    }

    public void loadPlan(int userId) {
        String weekStart = PlanEngine.getCurrentWeekStart();
        todayPlan = planRepo.getTodayPlan(userId);
        weekPlan  = planRepo.getWeekPlan(userId, weekStart);
    }

    // Build greeting based on time of day
    public String getGreeting(String name) {
        int hour = java.util.Calendar.getInstance()
                .get(java.util.Calendar.HOUR_OF_DAY);
        String timeGreeting = hour < 12 ? "Good morning"
                : hour < 17 ? "Good afternoon" : "Good evening";
        return timeGreeting + ", " + name + " 👋";
    }

    // Week number label e.g. "Week 3 · Day 2"
    public void loadWeekLabel(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int weekNum = db.plannedWorkoutDao().getLatestPlanWeek(userId);
            String today = PlanEngine.getTodayDayOfWeek();
            String[] days = {"MON","TUE","WED","THU","FRI","SAT","SUN"};
            int dayNum = 1;
            for (int i = 0; i < days.length; i++) {
                if (days[i].equals(today)) { dayNum = i + 1; break; }
            }
            coachMessage.postValue("Week " + weekNum + " · Day " + dayNum);
        });
    }

    // Check if today's workout is already logged
    public LiveData<WorkoutLog> getTodayLog(int userId) {
        String today = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        return androidx.lifecycle.Transformations.map(
                db.workoutLogDao().getAllLogs(userId),
                logs -> {
                    if (logs == null) return null;
                    for (WorkoutLog log : logs) {
                        if (log.date.equals(today)) return log;
                    }
                    return null;
                });
    }
}
