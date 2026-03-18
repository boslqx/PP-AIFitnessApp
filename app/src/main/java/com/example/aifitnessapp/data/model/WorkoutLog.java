package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_logs")
public class WorkoutLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int plannedWorkoutId; // FK → PlannedWorkout.id

    public String date;          // ISO "YYYY-MM-DD"

    // User's response
    public String completionStatus; // COMPLETED, MODIFIED, SKIPPED
    public int perceivedEffort;     // 1–5 (1=very easy, 5=very hard)
    public String notes;

    public String loggedAt;     // full timestamp
}