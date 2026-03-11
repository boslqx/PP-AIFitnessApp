package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit_log")
public class HabitLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String date;
    public String habitName;      // "Morning workout", "Drink 2L water"
    public boolean completed;
    public String category;       // "exercise", "nutrition", "sleep", "mindset"
    public long loggedAt;
}


