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
import com.example.aifitnessapp.engine.CoachEngine;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.List;

public class CoachViewModel extends AndroidViewModel {

    private FitAIDatabase db;

    public LiveData<UserPreferences>       currentUser;
    public MutableLiveData<CoachEngine.CoachReport> report = new MutableLiveData<>();
    public MutableLiveData<Boolean>        isLoading = new MutableLiveData<>(true);

    public CoachViewModel(@NonNull Application application) {
        super(application);
        db = FitAIDatabase.getInstance(application);
        currentUser = db.userPreferencesDao().getCurrentUser();
    }

    public void loadReport(int userId) {
        isLoading.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {

            UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
            if (prefs == null) {
                isLoading.postValue(false);
                return;
            }

            String weekStart = PlanEngine.getCurrentWeekStart();

            // Get this week's planned workouts
            List<PlannedWorkout> plans =
                    db.plannedWorkoutDao().getWeekPlanSync(userId, weekStart);

            // Get this week's logs
            List<WorkoutLog> logs =
                    db.workoutLogDao().getLogsSince(userId, weekStart);

            // Run analysis
            CoachEngine.CoachReport r = CoachEngine.analyse(prefs, plans, logs);

            report.postValue(r);
            isLoading.postValue(false);
        });
    }
}