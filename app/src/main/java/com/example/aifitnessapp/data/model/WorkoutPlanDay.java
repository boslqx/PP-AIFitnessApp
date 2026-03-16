package com.example.aifitnessapp.data.model;

/*
 * Represents one day in the AI-generated workout plan.
 * NOT stored in Room — plans are regenerated fresh each time
 * based on current performance data.
 *
 * WHY NOT STORE IT:
 * The plan should always reflect your latest data.
 * Storing it would mean showing stale recommendations.
 */
public class WorkoutPlanDay {

    public int    dayNumber;      // 1–7 (1 = today)
    public String dayLabel;       // "Today", "Tomorrow", "Wed Mar 18"
    public String workoutType;    // "strength", "cardio", "flexibility", "rest"
    public String intensity;      // "Light", "Moderate", "Hard"
    public int    intensityLevel; // 1–5 (for ConsistencyEngine compatibility)
    public int    durationMinutes;
    public String focus;          // e.g. "Upper Body", "HIIT", "Active Recovery"
    public String reasoning;      // WHY the AI chose this — shown to user
    public boolean isRestDay;
}