package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.DailyLog;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.data.model.WorkoutSession;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogViewModel extends AndroidViewModel {

    private FitAIDatabase db;

    public LiveData<UserProfile> currentUser;
    public MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);

    // Holds the saved log ID so SummaryActivity can load it
    public MutableLiveData<Integer> savedLogId = new MutableLiveData<>(-1);

    public LogViewModel(@NonNull Application application) {
        super(application);
        db = FitAIDatabase.getInstance(application);
        currentUser = db.userProfileDao().getCurrentUser();
    }

    public void saveLog(
            int userId,
            int calories, float protein, float carbs, float fat,
            int sleepHours, int sleepQuality,
            int waterMl,
            String mood,
            String workoutType, int durationMinutes, int intensityLevel
    ) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        AppExecutors.getInstance().diskIO().execute(() -> {

            // 1. Save the DailyLog
            DailyLog log = new DailyLog();
            log.userId           = userId;
            log.date             = today;
            log.caloriesConsumed = calories;
            log.proteinGrams     = protein;
            log.carbsGrams       = carbs;
            log.fatGrams         = fat;
            log.sleepHours       = sleepHours;
            log.sleepQuality     = sleepQuality;
            log.waterMl          = waterMl;
            log.mood             = mood;
            log.loggedAt         = System.currentTimeMillis();
            db.dailyLogDao().insert(log);

            // 2. Save the WorkoutSession (if user logged a workout)
            if (!workoutType.equals("rest") && durationMinutes > 0) {
                WorkoutSession session = new WorkoutSession();
                session.userId          = userId;
                session.date            = today;
                session.workoutType     = workoutType;
                session.durationMinutes = durationMinutes;
                session.intensityLevel  = intensityLevel;
                session.caloriesBurned  = estimateCaloriesBurned(
                        workoutType, durationMinutes, intensityLevel);
                session.completed       = true;
                session.wasPlanned      = false;
                session.createdAt       = System.currentTimeMillis();
                db.workoutSessionDao().insert(session);
            }

            saveSuccess.postValue(true);
        });
    }

    // Simple estimate — will be replaced by ML model in Phase 3
    private int estimateCaloriesBurned(String type, int minutes, int intensity) {
        // Base calories per minute by workout type
        float basePerMin;
        switch (type) {
            case "strength":    basePerMin = 6f;  break;
            case "cardio":      basePerMin = 9f;  break;
            case "flexibility": basePerMin = 3f;  break;
            default:            basePerMin = 5f;
        }
        // Multiply by intensity factor (1→0.7, 2→0.85, 3→1.0, 4→1.2, 5→1.4)
        float intensityFactor = 0.55f + (intensity * 0.15f);
        return Math.round(basePerMin * minutes * intensityFactor);
    }

    public String getTodayDate() {
        return new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date());
    }
}