package com.example.aifitnessapp.repository;

import android.app.Application;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.ConsistencyScore;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.data.model.WorkoutPlan;
import com.example.aifitnessapp.data.model.WorkoutSession;
import com.example.aifitnessapp.engine.WorkoutPlanEngine;
import java.util.List;

public class WorkoutPlanRepository {

    private FitAIDatabase db;

    public WorkoutPlanRepository(Application application) {
        db = FitAIDatabase.getInstance(application);
    }

    /*
     * Fetches all required data from DB, then calls the engine.
     * Must run on a background thread (called from ViewModel diskIO).
     *
     * DATA FLOW:
     *   DB → sessions + scores + profile → WorkoutPlanEngine → WorkoutPlan
     */
    public WorkoutPlan generatePlanForUser(int userId) {
        UserProfile profile = db.userProfileDao().getUserByIdSync(userId);
        if (profile == null) return null;

        List<WorkoutSession> sessions =
                db.workoutSessionDao().getAllSessionsSync(userId);

        List<ConsistencyScore> scores =
                db.consistencyScoreDao().getLast7ScoresSync(userId);

        return WorkoutPlanEngine.generatePlan(profile, sessions, scores);
    }
}