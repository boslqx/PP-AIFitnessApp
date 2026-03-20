package com.example.aifitnessapp.util;

public class WorkoutTimer {
    public static String format(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}