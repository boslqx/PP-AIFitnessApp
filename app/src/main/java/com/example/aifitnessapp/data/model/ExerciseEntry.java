package com.example.aifitnessapp.data.model;

/*
 * Represents one exercise item in the log checklist.
 * Parsed from PlannedWorkout.sessionDetail at runtime.
 * NOT stored in Room — saved as a JSON string inside WorkoutLog.notes.
 */
public class ExerciseEntry {
    public String name;       // e.g. "Squat"
    public String original;   // e.g. "4×5" — what was planned
    public String actual;     // e.g. "3×5" — what user actually did (editable)
    public boolean completed; // ticked or not

    public ExerciseEntry(String name, String planned) {
        this.name      = name;
        this.original  = planned;
        this.actual    = planned; // defaults to planned, user can edit
        this.completed = false;
    }
}