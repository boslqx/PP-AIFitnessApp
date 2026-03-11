package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_session")
public class WorkoutSession {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String date;
    public String workoutType;    // "strength", "cardio", "flexibility", "rest"
    public int durationMinutes;
    public int intensityLevel;    // 1–5
    public int caloriesBurned;
    public boolean completed;
    public boolean wasPlanned;    // AI-planned vs user-added
    public String notes;
    public long createdAt;
}