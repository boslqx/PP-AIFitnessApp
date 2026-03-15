package com.example.aifitnessapp.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.db.dao.WorkoutSessionDao;
import com.example.aifitnessapp.data.model.WorkoutSession;
import java.util.List;

public class WorkoutHistoryRepository {

    private WorkoutSessionDao workoutDao;

    public WorkoutHistoryRepository(Application application) {
        workoutDao = FitAIDatabase.getInstance(application).workoutSessionDao();
    }

    /*
     * LiveData — Room automatically re-delivers this whenever
     * the workout_session table changes (e.g. after a new log is saved).
     * The Activity observes this and rebuilds the list on every update.
     */
    public LiveData<List<WorkoutSession>> getLast30Sessions(int userId) {
        return workoutDao.getLast30Sessions(userId);
    }

    // Plain list for summary stats (runs on diskIO)
    public List<WorkoutSession> getAllSessionsSync(int userId) {
        return workoutDao.getAllSessionsSync(userId);
    }
}