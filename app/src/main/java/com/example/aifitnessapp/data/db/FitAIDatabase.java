package com.example.aifitnessapp.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.aifitnessapp.data.db.dao.*;
import com.example.aifitnessapp.data.model.*;

/*
 * FitAIDatabase — the single Room database for the entire app.
 *
 * VERSION HISTORY:
 *  v1 — initial schema (UserPreferences, PlannedWorkout, WorkoutLog)
 *  v2 — added fields (various iterations during development)
 *  v3 — added fields
 *  v4 — added fields
 *  v5 — added User entity for authentication ← current
 *
 * fallbackToDestructiveMigration():
 * During development, if the schema changes and no migration is written,
 * Room drops and recreates all tables. Data is lost, but the app works.
 * For a production app, replace this with proper @Migration classes.
 *
 * SINGLETON PATTERN:
 * Only one instance of the database should ever exist.
 * Creating multiple instances causes data inconsistency and wastes memory.
 * The volatile + double-checked locking ensures thread safety:
 * if two threads call getInstance() simultaneously, only one creates the db.
 */
@Database(
        entities = {
                User.class,             // ← NEW: authentication
                UserPreferences.class,
                PlannedWorkout.class,
                WorkoutLog.class
        },
        version = 5,                    // ← bumped from 4 to 5
        exportSchema = false
)
public abstract class FitAIDatabase extends RoomDatabase {

    private static volatile FitAIDatabase INSTANCE;

    // ── DAOs ──────────────────────────────────────────────────
    public abstract UserDao          userDao();           // ← NEW
    public abstract UserPreferencesDao userPreferencesDao();
    public abstract PlannedWorkoutDao  plannedWorkoutDao();
    public abstract WorkoutLogDao      workoutLogDao();

    // ── Singleton ─────────────────────────────────────────────
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