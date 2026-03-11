package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "consistency_score")
public class ConsistencyScore {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String date;

    public float score;
    public int weekStreak;
    public float completionRate;
    public float intensityFactor;

    public String trend;
}