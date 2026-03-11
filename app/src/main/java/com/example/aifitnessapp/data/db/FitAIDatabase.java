package com.example.aifitnessapp.data.db;

import android.content.Context;
import androidx.room.*;
import com.example.aifitnessapp.data.model.*;
import com.example.aifitnessapp.data.db.dao.*;

@Database(
        entities = {
                UserProfile.class,
                DailyLog.class,
                WorkoutSession.class,
                HabitLog.class,
                ConsistencyScore.class
        },
        version = 1,
        exportSchema = true
)
public abstract class FitAIDatabase extends RoomDatabase {

    // Abstract methods — Room implements these automatically
    public abstract UserProfileDao userProfileDao();
    public abstract DailyLogDao dailyLogDao();
    public abstract WorkoutSessionDao workoutSessionDao();
    public abstract HabitLogDao habitLogDao();
    public abstract ConsistencyScoreDao consistencyScoreDao();

    // Singleton: only ONE database instance for the entire app
    private static volatile FitAIDatabase instance;

    public static synchronized FitAIDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),  // use app context, NOT activity context
                            FitAIDatabase.class,
                            "fitai_database"                  // filename on device storage
                    )
                    .fallbackToDestructiveMigration()     // Dev only: wipes DB on schema change
                    .build();
        }
        return instance;
    }
}