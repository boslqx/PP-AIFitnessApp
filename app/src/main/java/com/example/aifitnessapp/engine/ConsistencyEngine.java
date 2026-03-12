package com.example.aifitnessapp.engine;

import com.example.aifitnessapp.data.model.ConsistencyScore;
import com.example.aifitnessapp.data.model.DailyLog;
import java.util.List;

public class ConsistencyEngine {

    /*
     * FORMULA:
     * Score = CompletionRate × IntensityFactor × StreakMultiplier × SleepFactor
     *
     * Each factor explained below.
     */
    public static float calculateDailyScore(
            int completedTasks,
            int plannedTasks,
            int intensityLevel,   // 1–5
            int currentStreak,    // consecutive days target was met
            int sleepHours
    ) {
        if (plannedTasks == 0) return 0f;

        // Base: what % of planned tasks did user complete? (0.0 to 1.0)
        float completionRate = (float) completedTasks / plannedTasks;

        // Intensity factor: reward harder effort
        // 1→0.8, 2→0.9, 3→1.0, 4→1.1, 5→1.2
        float intensityFactor = 0.7f + (intensityLevel * 0.1f);

        // Streak multiplier: consistency over time is rewarded
        // Caps at 2.0x for a 20-day streak
        float streakMultiplier = 1.0f + Math.min(currentStreak * 0.05f, 1.0f);

        // Sleep factor: poor sleep reduces score (research-backed)
        float sleepFactor;
        if      (sleepHours >= 8) sleepFactor = 1.1f;
        else if (sleepHours >= 7) sleepFactor = 1.0f;
        else if (sleepHours >= 6) sleepFactor = 0.9f;
        else if (sleepHours >= 5) sleepFactor = 0.75f;
        else                      sleepFactor = 0.6f;

        float raw = completionRate * intensityFactor * streakMultiplier * sleepFactor;
        return Math.min(raw * 100f, 100f); // scale to 0–100, cap at 100
    }

    // Average of the last 7 daily scores
    public static float calculateWeeklyScore(List<ConsistencyScore> weekScores) {
        if (weekScores == null || weekScores.isEmpty()) return 0f;
        float sum = 0;
        for (ConsistencyScore s : weekScores) sum += s.score;
        return sum / weekScores.size();
    }

    // Detects HIGH / MEDIUM / LOW burnout risk
    public static String detectBurnoutRisk(
            List<DailyLog> recentLogs,
            List<ConsistencyScore> recentScores
    ) {
        if (recentLogs == null || recentLogs.isEmpty()) return "LOW";

        // Factor 1: Average sleep
        float avgSleep = 0;
        for (DailyLog log : recentLogs) avgSleep += log.sleepHours;
        avgSleep /= recentLogs.size();

        // Factor 2: How many days had a very low score
        int lowScoreDays = 0;
        for (ConsistencyScore s : recentScores) {
            if (s.score < 40f) lowScoreDays++;
        }

        // Factor 3: Declining trend (compare first half vs second half of scores)
        boolean declining = false;
        if (recentScores.size() >= 4) {
            int half = recentScores.size() / 2;
            float recent = 0, older = 0;
            for (int i = 0; i < half; i++)        recent += recentScores.get(i).score;
            for (int i = half; i < recentScores.size(); i++) older += recentScores.get(i).score;
            declining = (recent / half) < (older / half) - 10f;
        }

        // Risk score: add up warning signals
        int riskPoints = 0;
        if (avgSleep < 6f)     riskPoints += 3;
        if (lowScoreDays >= 3) riskPoints += 3;
        if (declining)         riskPoints += 2;

        if (riskPoints >= 6) return "HIGH";
        if (riskPoints >= 3) return "MEDIUM";
        return "LOW";
    }
}