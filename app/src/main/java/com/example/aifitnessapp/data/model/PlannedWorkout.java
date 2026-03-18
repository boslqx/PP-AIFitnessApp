package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "planned_workouts")
public class PlannedWorkout {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int planWeek;         // 1, 2, 3... which week of the program

    public String dayOfWeek;     // MON, TUE, WED, THU, FRI, SAT, SUN
    public String weekStartDate; // ISO "YYYY-MM-DD" — Monday of that week

    public String activityType;  // GYM, RUNNING, BOULDERING, CYCLING,
    // SWIMMING, YOGA, HOME, SPORTS, REST

    public String sessionTitle;  // e.g. "Upper Body Push"
    public String sessionDetail; // e.g. "3x bench press, shoulder press, tricep dips"
    public String coachNote;     // AI reasoning: "You've been consistent, stepping up"

    public int intensityLevel;   // 1–5
    public int durationMinutes;
    public boolean isRestDay;

    public String createdAt;
}