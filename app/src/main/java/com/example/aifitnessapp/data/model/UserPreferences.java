package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_preferences")
public class UserPreferences {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Step 1 — Goal
    public String fitnessGoal; // LOSE_FAT, BUILD_MUSCLE, ENDURANCE, GENERAL

    // Step 2 — Activities (comma-separated)
    public String selectedActivities; // e.g. "GYM,RUNNING,BOULDERING"

    // Step 3 — Activity-specific
    public String gymEquipment;  // FULL_GYM, DUMBBELLS, BARBELL_RACK, MACHINES, NONE
    public String sports;        // e.g. "Basketball,Football" (nullable)
    public boolean tracksRunningPace;

    // Step 4 — Fitness level
    public String fitnessLevel;  // BEGINNER, INTERMEDIATE, ADVANCED

    // Step 5 — Commitment
    public int daysPerWeek;      // 2–6

    // Step 6 — Bio
    public String name;
    public int age;
    public float weightKg;

    // Meta
    public String createdAt;
    public String updatedAt;

    // Helper: check if a specific activity is selected
    public boolean hasActivity(String activity) {
        if (selectedActivities == null) return false;
        return selectedActivities.contains(activity);
    }

    // Helper: get activities as array
    public String[] getActivitiesArray() {
        if (selectedActivities == null) return new String[0];
        return selectedActivities.split(",");
    }
}