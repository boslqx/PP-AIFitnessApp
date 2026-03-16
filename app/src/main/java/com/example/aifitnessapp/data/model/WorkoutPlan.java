package com.example.aifitnessapp.data.model;

import java.util.List;

/*
 * A complete 7-day AI-generated plan.
 * Contains the days plus metadata about how it was generated.
 */
public class WorkoutPlan {

    public List<WorkoutPlanDay> days;

    // Metadata shown in the plan header
    public String planTitle;       // e.g. "Recovery Week" or "Build Phase"
    public String planDescription; // overall strategy explanation
    public int    weeklyTarget;    // workouts/week AI is targeting
    public float  currentScore;    // consistency score this plan is based on
    public String adaptationNote;  // e.g. "Reduced intensity — burnout risk detected"
}