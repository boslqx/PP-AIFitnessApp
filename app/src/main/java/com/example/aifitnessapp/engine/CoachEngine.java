package com.example.aifitnessapp.engine;

import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import java.util.List;

/*
 * Analyses this week's logs and generates:
 *  - A coach message (personalised feedback)
 *  - An adaptation notice (what changes next week)
 *  - A streak count
 *  - Average perceived effort
 *  - Completion rate
 */
public class CoachEngine {

    public static class CoachReport {
        public String coachMessage;
        public String adaptationNotice;
        public String adaptationStrategy; // RECOVERY, MAINTAIN, PROGRESSIVE, CATCH_UP
        public int    streakDays;
        public float  avgEffort;          // 1.0–5.0
        public int    completedWorkouts;
        public int    plannedWorkouts;
        public float  completionRate;     // 0.0–1.0
        public int    skippedWorkouts;
        public String nextIntensityLabel;
    }

    public static CoachReport analyse(
            UserPreferences prefs,
            List<PlannedWorkout> weekPlans,
            List<WorkoutLog>     weekLogs) {

        CoachReport report = new CoachReport();

        // ── Count planned vs completed ──────────────────────
        int planned   = 0;
        int completed = 0;
        int skipped   = 0;
        float totalEffort = 0;
        int effortCount   = 0;

        // Count non-rest planned workouts
        for (PlannedWorkout p : weekPlans) {
            if (!p.isRestDay) planned++;
        }

        // Analyse logs
        for (WorkoutLog log : weekLogs) {
            switch (log.completionStatus) {
                case "COMPLETED":
                case "MODIFIED":
                    completed++;
                    if (log.perceivedEffort > 0) {
                        totalEffort += log.perceivedEffort;
                        effortCount++;
                    }
                    break;
                case "SKIPPED":
                    skipped++;
                    break;
            }
        }

        report.plannedWorkouts  = planned;
        report.completedWorkouts = completed;
        report.skippedWorkouts  = skipped;
        report.completionRate   = planned > 0 ? (float) completed / planned : 0f;
        report.avgEffort        = effortCount > 0 ? totalEffort / effortCount : 0f;

        // ── Streak ──────────────────────────────────────────
        report.streakDays = calculateStreak(weekLogs);

        // ── Adaptation strategy ─────────────────────────────
        int nextIntensity = PlanEngine.resolveIntensity(prefs, weekLogs);
        report.adaptationStrategy = determineStrategy(
                completed, planned, skipped, report.avgEffort, report.streakDays);
        report.nextIntensityLabel = intensityLabel(nextIntensity);

        // ── Adaptation notice ───────────────────────────────
        report.adaptationNotice = buildAdaptationNotice(
                report.adaptationStrategy, nextIntensity, completed, planned);

        // ── Coach message ───────────────────────────────────
        report.coachMessage = buildCoachMessage(
                prefs, report, completed, planned);

        return report;
    }

    // ── Strategy determination ───────────────────────────────

    private static String determineStrategy(int completed, int planned,
                                            int skipped, float avgEffort, int streak) {
        if (skipped >= 2) return "RECOVERY";
        if (completed >= planned && avgEffort <= 2.5f && planned > 0) return "PROGRESSIVE";
        if (completed < planned - 1 && skipped < 2) return "CATCH_UP";
        return "MAINTAIN";
    }

    // ── Adaptation notice ────────────────────────────────────

    private static String buildAdaptationNotice(String strategy,
                                                int nextIntensity,
                                                int completed, int planned) {
        switch (strategy) {
            case "RECOVERY":
                return "⚠️ Recovery week ahead — intensity reduced to help you "
                        + "bounce back without risk of injury.";
            case "PROGRESSIVE":
                return "🔥 You're crushing it! Next week steps up to "
                        + intensityLabel(nextIntensity)
                        + " intensity — you've earned it.";
            case "CATCH_UP":
                return "💪 You've completed " + completed + " of " + planned
                        + " workouts. Next week adds a catch-up opportunity "
                        + "to get back on target.";
            default:
                return "✅ You're on track. Next week maintains "
                        + intensityLabel(nextIntensity)
                        + " intensity with the same structure.";
        }
    }

    // ── Coach message ────────────────────────────────────────

    private static String buildCoachMessage(UserPreferences prefs,
                                            CoachReport report,
                                            int completed, int planned) {
        String name = prefs.name != null ? prefs.name : "there";

        // No logs yet
        if (completed == 0 && report.skippedWorkouts == 0) {
            return "Hey " + name + "! Your plan is ready. "
                    + "Complete your first session and come back here "
                    + "to see your personalised feedback.";
        }

        // Skipped most sessions
        if (report.skippedWorkouts >= 2) {
            return "Hey " + name + ", life happens — no judgement. "
                    + "The best workout is the one you actually do. "
                    + "Next week's plan is lighter to help you get back in the rhythm.";
        }

        // All completed, felt easy
        if (completed >= planned && report.avgEffort <= 2.0f && planned > 0) {
            return "Impressive week, " + name + "! You completed everything "
                    + "and it felt easy — that's a clear sign you're ready to level up. "
                    + "Expect harder sessions next week.";
        }

        // All completed, felt hard
        if (completed >= planned && report.avgEffort >= 4.0f && planned > 0) {
            return "Great effort this week, " + name + "! You pushed hard "
                    + "and completed everything. Recovery tonight is important — "
                    + "sleep and nutrition matter as much as the workout.";
        }

        // Partial completion
        if (completed > 0 && completed < planned) {
            return "Good start, " + name + "! You showed up "
                    + completed + " out of " + planned + " times — "
                    + "that's real progress. Consistency over perfection, always.";
        }

        // Good streak
        if (report.streakDays >= 5) {
            return "🔥 " + report.streakDays + "-day streak, " + name
                    + "! That level of consistency is how real change happens. "
                    + "Keep protecting that streak.";
        }

        // Default
        return "Keep going, " + name + "! Every session logged "
                + "helps the AI understand your body better and "
                + "build a smarter plan for next week.";
    }

    // ── Streak calculation ───────────────────────────────────

    private static int calculateStreak(List<WorkoutLog> logs) {
        if (logs == null || logs.isEmpty()) return 0;
        int streak = 0;
        for (WorkoutLog log : logs) {
            if ("COMPLETED".equals(log.completionStatus)
                    || "MODIFIED".equals(log.completionStatus)) {
                streak++;
            }
        }
        return streak;
    }

    // ── Helpers ──────────────────────────────────────────────

    private static String intensityLabel(int level) {
        switch (level) {
            case 1: return "Very Light";
            case 2: return "Light";
            case 3: return "Moderate";
            case 4: return "Hard";
            case 5: return "Very Hard";
            default: return "Moderate";
        }
    }
}