package com.example.aifitnessapp.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.aifitnessapp.data.db.dao.*;
import com.example.aifitnessapp.data.model.*;

@Database(
        entities = {
                UserPreferences.class,
                PlannedWorkout.class,
                WorkoutLog.class
        },
        version = 4,
        exportSchema = false
)
public abstract class FitAIDatabase extends RoomDatabase {

    private static volatile FitAIDatabase INSTANCE;

    public abstract UserPreferencesDao userPreferencesDao();
    public abstract PlannedWorkoutDao plannedWorkoutDao();
    public abstract WorkoutLogDao workoutLogDao();

    public static FitAIDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FitAIDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FitAIDatabase.class,
                                    "fitai_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}