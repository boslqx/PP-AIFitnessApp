package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_log")
public class DailyLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;            // Links to UserProfile.id
    public String date;           // "2025-03-11" — ISO 8601 format
    public float weightKg;        // Optional: morning weigh-in
    public int caloriesConsumed;
    public float proteinGrams;
    public float carbsGrams;
    public float fatGrams;
    public int sleepHours;        // 0–12
    public int sleepQuality;      // 1–5 star rating
    public int waterMl;
    public String mood;           // "great", "okay", "tired", "stressed"
    public String notes;
    public long loggedAt;
}