package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogViewModel extends AndroidViewModel {

    private FitAIDatabase db;
    public LiveData<UserPreferences> currentUser;
    public MutableLiveData<Boolean>  saveSuccess = new MutableLiveData<>(null);

    // Holds the photo path chosen by user
    public MutableLiveData<String> photoPath = new MutableLiveData<>(null);

    public LogViewModel(@NonNull Application application) {
        super(application);
        db = FitAIDatabase.getInstance(application);
        currentUser = db.userPreferencesDao().getCurrentUser();
    }

    public void saveLog(int userId, int plannedWorkoutId,
                        String completionStatus, int perceivedEffort,
                        String notes, String photoPath) {

        AppExecutors.getInstance().diskIO().execute(() -> {
            WorkoutLog log    = new WorkoutLog();
            log.userId        = userId;
            log.plannedWorkoutId = plannedWorkoutId;
            log.completionStatus = completionStatus;
            log.perceivedEffort  = perceivedEffort;
            log.notes         = notes;
            log.photoPath     = photoPath;
            log.date          = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()).format(new Date());
            log.loggedAt      = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            db.workoutLogDao().insert(log);
            saveSuccess.postValue(true);
        });
    }
}