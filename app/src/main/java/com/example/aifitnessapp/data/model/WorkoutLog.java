package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_logs")
public class WorkoutLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int plannedWorkoutId;

    public String date;
    public String completionStatus; // COMPLETED, MODIFIED, SKIPPED
    public int    perceivedEffort;  // 1–5
    public String notes;            // serialized exercise list + user notes
    public String photoPath;        // file path to saved photo (nullable)
    public String loggedAt;
}