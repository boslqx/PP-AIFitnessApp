package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Bio data (user fills in during onboarding)
    public String name;
    public float weight;         // kg
    public float height;         // cm
    public int age;
    public String gender;        // "male" or "female"
    public String activityLevel; // "sedentary", "light", "moderate", "active", "very_active"
    public String fitnessGoal;   // "fat_loss", "muscle_gain", "endurance"
    public int timelineMonths;   // 3 or 6
    public float targetWeight;   // user's goal weight

    // AI-calculated fields (computed once, stored for performance)
    public int dailyCalorieTarget;
    public int workoutFrequencyPerWeek;

    public long createdAt;       // Unix timestamp — System.currentTimeMillis()
}