package com.example.aifitnessapp.engine;

import com.example.aifitnessapp.data.model.ConsistencyScore;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.data.model.WorkoutPlan;
import com.example.aifitnessapp.data.model.WorkoutPlanDay;
import com.example.aifitnessapp.data.model.WorkoutSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutPlanEngine {

    /*
     * ═══════════════════════════════════════════════════════
     *  MAIN ENTRY POINT
     *  Called from WorkoutPlanRepository on a background thread.
     *
     *  INPUTS:
     *   - user profile     → goal, target frequency, fitness level
     *   - recent sessions  → what the user actually did last 7 days
     *   - recent scores    → consistency trend + burnout risk
     *
     *  OUTPUT:
     *   - WorkoutPlan with 7 WorkoutPlanDay objects
     * ═══════════════════════════════════════════════════════
     */
    public static WorkoutPlan generatePlan(
            UserProfile profile,
            List<WorkoutSession> recentSessions,
            List<ConsistencyScore> recentScores
    ) {
        // ── Step 1: Analyse recent performance ──────────────
        PerformanceSnapshot snap = analysePerformance(
                profile, recentSessions, recentScores);

        // ── Step 2: Choose adaptation strategy ──────────────
        AdaptationStrategy strategy = chooseStrategy(snap);

        // ── Step 3: Build the 7-day schedule ────────────────
        List<WorkoutPlanDay> days = buildSchedule(profile, snap, strategy);

        // ── Step 4: Package into WorkoutPlan ────────────────
        WorkoutPlan plan = new WorkoutPlan();
        plan.days           = days;
        plan.weeklyTarget   = snap.targetFrequency;
        plan.currentScore   = snap.avgScore;
        plan.planTitle      = strategy.planTitle;
        plan.planDescription = strategy.description;
        plan.adaptationNote = strategy.adaptationNote;

        return plan;
    }

    // ════════════════════════════════════════════════════════
    //  STEP 1: PERFORMANCE SNAPSHOT
    //  Reads raw data and produces a simple summary object.
    // ════════════════════════════════════════════════════════

    private static PerformanceSnapshot analysePerformance(
            UserProfile profile,
            List<WorkoutSession> sessions,
            List<ConsistencyScore> scores
    ) {
        PerformanceSnapshot snap = new PerformanceSnapshot();
        snap.targetFrequency = profile.workoutFrequencyPerWeek;
        snap.goal            = profile.fitnessGoal;

        // Count completed workouts in the last 7 days
        String sevenDaysAgo = daysAgo(7);
        int completedThisWeek = 0;
        float totalIntensity  = 0;
        int   intensityCount  = 0;

        for (WorkoutSession s : sessions) {
            if (s.date.compareTo(sevenDaysAgo) >= 0 && s.completed
                    && !s.workoutType.equals("rest")) {
                completedThisWeek++;
                totalIntensity += s.intensityLevel;
                intensityCount++;
            }
        }

        snap.completedThisWeek = completedThisWeek;
        snap.avgIntensity = intensityCount > 0
                ? totalIntensity / intensityCount : 3f;

        // Consistency score average and trend
        if (!scores.isEmpty()) {
            float sum = 0;
            for (ConsistencyScore s : scores) sum += s.score;
            snap.avgScore = sum / scores.size();

            // Trend: compare most recent 3 vs oldest 3
            if (scores.size() >= 4) {
                float recent = (scores.get(0).score + scores.get(1).score) / 2f;
                float older  = (scores.get(scores.size()-2).score
                        + scores.get(scores.size()-1).score) / 2f;
                snap.scoreTrend = recent - older; // positive = improving
            }
        }

        // Burnout risk proxy: avg score < 40 for 3+ days
        int lowDays = 0;
        for (ConsistencyScore s : scores) if (s.score < 40f) lowDays++;
        snap.burnoutRisk = lowDays >= 3 ? "HIGH"
                : lowDays >= 2 ? "MEDIUM" : "LOW";

        // Days since last rest day
        snap.daysSinceRest = countDaysSinceRest(sessions);

        return snap;
    }

    // ════════════════════════════════════════════════════════
    //  STEP 2: ADAPTATION STRATEGY
    //
    //  This is the "intelligence" — we map the performance
    //  snapshot to one of 4 strategies:
    //
    //   RECOVERY   → burnout risk HIGH or too many consecutive days
    //   MAINTAIN   → on track, keep current load
    //   PROGRESSIVE → performing above target, increase load
    //   CATCH_UP   → below target, add volume carefully
    // ════════════════════════════════════════════════════════

    private static AdaptationStrategy chooseStrategy(PerformanceSnapshot snap) {
        AdaptationStrategy s = new AdaptationStrategy();

        // RECOVERY: burnout risk or 5+ consecutive days without rest
        if (snap.burnoutRisk.equals("HIGH") || snap.daysSinceRest >= 5) {
            s.type           = "RECOVERY";
            s.planTitle      = "Recovery Week";
            s.intensityMod   = -1;  // reduce intensity by 1 level
            s.frequencyMod   = -1;  // one fewer workout this week
            s.description    = "Your body needs recovery time. This week focuses on "
                    + "light activity and rest to prevent injury and rebuild energy.";
            s.adaptationNote = snap.daysSinceRest >= 5
                    ? "⚠️ " + snap.daysSinceRest + " consecutive days detected — rest scheduled."
                    : "⚠️ High burnout risk detected — intensity reduced.";

            // PROGRESSIVE: consistently above target + good scores + improving trend
        } else if (snap.completedThisWeek >= snap.targetFrequency
                && snap.avgScore >= 70f && snap.scoreTrend > 5f) {
            s.type           = "PROGRESSIVE";
            s.planTitle      = "Progressive Overload Phase";
            s.intensityMod   = +1; // increase intensity by 1 level
            s.frequencyMod   = 0;
            s.description    = "You're consistently hitting your targets with great scores. "
                    + "Time to increase the challenge and keep growing.";
            s.adaptationNote = "🔥 Scores improving — intensity increased this week.";

            // CATCH_UP: below target but not burnt out
        } else if (snap.completedThisWeek < snap.targetFrequency - 1
                && !snap.burnoutRisk.equals("HIGH")) {
            s.type           = "CATCH_UP";
            s.planTitle      = "Get Back on Track";
            s.intensityMod   = 0;
            s.frequencyMod   = +1; // one extra workout opportunity
            s.description    = "You've missed a few sessions this week. "
                    + "This plan adds an extra workout opportunity "
                    + "to help you hit your weekly target.";
            s.adaptationNote = "💪 " + snap.completedThisWeek + " / "
                    + snap.targetFrequency + " workouts done — adding catch-up day.";

            // MAINTAIN: on track
        } else {
            s.type           = "MAINTAIN";
            s.planTitle      = "Steady Progress";
            s.intensityMod   = 0;
            s.frequencyMod   = 0;
            s.description    = "You're right on track. This week maintains your "
                    + "current training load with smart recovery built in.";
            s.adaptationNote = "✅ Consistency score: " + Math.round(snap.avgScore)
                    + " / 100 — keep it up!";
        }

        return s;
    }

    // ════════════════════════════════════════════════════════
    //  STEP 3: BUILD THE 7-DAY SCHEDULE
    //
    //  Uses the goal + strategy to lay out workout types,
    //  intensities, and rest days across the week.
    // ════════════════════════════════════════════════════════

    private static List<WorkoutPlanDay> buildSchedule(
            UserProfile profile,
            PerformanceSnapshot snap,
            AdaptationStrategy strategy
    ) {
        int targetWorkouts = Math.max(1,
                snap.targetFrequency + strategy.frequencyMod);

        // Clamp to 1–6 workouts per week
        targetWorkouts = Math.min(targetWorkouts, 6);

        // Base intensity from recent performance, adjusted by strategy
        int baseIntensity = Math.round(snap.avgIntensity) + strategy.intensityMod;
        baseIntensity = Math.max(1, Math.min(5, baseIntensity)); // clamp 1–5

        // Get workout type rotation for this goal
        String[] typeRotation = getTypeRotation(profile.fitnessGoal);

        List<WorkoutPlanDay> days = new ArrayList<>();

        // Place workout days and rest days across 7 days
        // Rest days are distributed evenly between workout days
        boolean[] isWorkout = distributeWorkouts(targetWorkouts);

        int workoutIndex = 0;
        for (int i = 0; i < 7; i++) {
            WorkoutPlanDay day = new WorkoutPlanDay();
            day.dayNumber  = i + 1;
            day.dayLabel   = getDayLabel(i);

            if (isWorkout[i]) {
                String type = typeRotation[workoutIndex % typeRotation.length];
                workoutIndex++;

                day.workoutType    = type;
                day.isRestDay      = false;
                day.intensityLevel = adjustIntensityForDay(baseIntensity, i, type);
                day.intensity      = intensityLabel(day.intensityLevel);
                day.durationMinutes = durationForIntensity(day.intensityLevel, type);
                day.focus          = focusForType(type, workoutIndex, profile.fitnessGoal);
                day.reasoning      = buildReasoning(type, day.intensityLevel,
                        strategy.type, snap);
            } else {
                day.workoutType     = "rest";
                day.isRestDay       = true;
                day.intensityLevel  = 0;
                day.intensity       = "Rest";
                day.durationMinutes = 0;
                day.focus           = "Recovery";
                day.reasoning       = "Rest is essential. Muscles grow during recovery, "
                        + "not during the workout itself.";
            }

            days.add(day);
        }

        return days;
    }

    // ════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════════════════════

    /*
     * Returns the workout type rotation for a given goal.
     * The array is cycled through as workout days are assigned.
     *
     * fat_loss:    alternates cardio + strength (HIIT approach)
     * muscle_gain: strength-focused, 1 cardio per week
     * endurance:   cardio-heavy, some flexibility
     */
    private static String[] getTypeRotation(String goal) {
        switch (goal) {
            case "fat_loss":
                return new String[]{"cardio", "strength", "cardio",
                        "strength", "flexibility", "cardio"};
            case "muscle_gain":
                return new String[]{"strength", "strength", "cardio",
                        "strength", "strength", "flexibility"};
            case "endurance":
            default:
                return new String[]{"cardio", "cardio", "flexibility",
                        "cardio", "strength", "cardio"};
        }
    }

    /*
     * Distributes workout days evenly across the 7-day week.
     * e.g. 4 workouts → days 0, 1, 3, 5 (with rest gaps)
     *
     * ALGORITHM:
     * We space workouts as evenly as possible using integer division.
     * This avoids back-to-back workout days when possible.
     */
    private static boolean[] distributeWorkouts(int count) {
        boolean[] days = new boolean[7];
        // Place workouts at evenly spaced intervals
        for (int i = 0; i < count; i++) {
            int slot = Math.round(i * (7f / count));
            // Avoid placing on an already-used slot
            while (slot < 7 && days[slot]) slot++;
            if (slot < 7) days[slot] = true;
        }
        return days;
    }

    /*
     * Intensity varies slightly across the week:
     * - Day 1 (today) starts moderate
     * - Mid-week peak (day 3-4)
     * - Taper toward end of week
     * - Flexibility days always get lower intensity
     */
    private static int adjustIntensityForDay(int base, int dayIndex, String type) {
        if (type.equals("flexibility")) return Math.max(1, base - 1);
        int[] modifier = {0, 0, +1, +1, 0, -1, -1};
        int adjusted = base + modifier[dayIndex];
        return Math.max(1, Math.min(5, adjusted));
    }

    /*
     * Workout duration scales with intensity and type.
     * Strength: longer at lower intensity (more volume)
     * Cardio: shorter but higher intensity possible
     */
    private static int durationForIntensity(int intensity, String type) {
        int base;
        switch (type) {
            case "strength":    base = 50; break;
            case "cardio":      base = 35; break;
            case "flexibility": base = 25; break;
            default:            base = 40;
        }
        // Intensity 1=−10min, 2=−5min, 3=base, 4=+5min, 5=+10min
        return base + (intensity - 3) * 5;
    }

    /*
     * Returns a specific focus area to keep workouts varied.
     * Rotates through body parts / cardio styles.
     */
    private static String focusForType(String type, int index, String goal) {
        switch (type) {
            case "strength":
                String[] strengthFocus = {"Upper Body", "Lower Body", "Full Body",
                        "Push (Chest/Shoulders)", "Pull (Back/Biceps)"};
                return strengthFocus[index % strengthFocus.length];
            case "cardio":
                String[] cardioFocus = {"Steady State", "HIIT", "Tempo Run",
                        "Cycling / Swimming", "Circuit Training"};
                return cardioFocus[index % cardioFocus.length];
            case "flexibility":
                return "Yoga / Stretching";
            default:
                return "General Fitness";
        }
    }

    /*
     * Builds the "why" explanation shown under each day's card.
     * This makes the AI feel transparent and trustworthy.
     */
    private static String buildReasoning(String type, int intensity,
                                         String strategyType, PerformanceSnapshot snap) {
        String intensityWord = intensityLabel(intensity).toLowerCase();

        switch (strategyType) {
            case "RECOVERY":
                return type.equals("flexibility")
                        ? "Active recovery — gentle movement improves blood flow without taxing muscles."
                        : "Keeping intensity low this week. Your scores suggest your body needs recovery.";
            case "PROGRESSIVE":
                return "Intensity increased from your usual " + Math.round(snap.avgIntensity)
                        + "/5 → " + intensity + "/5. You've earned it with consistent scores.";
            case "CATCH_UP":
                return "Added to help reach your weekly target of "
                        + snap.targetFrequency + " sessions.";
            default: // MAINTAIN
                return "Scheduled " + intensityWord + " " + type
                        + " to maintain your current training load.";
        }
    }

    private static int countDaysSinceRest(List<WorkoutSession> sessions) {
        int count = 0;
        String today = todayStr();
        // Walk backwards from today
        for (int i = 0; i < 7; i++) {
            String date = daysAgo(i);
            boolean hadRest = false;
            for (WorkoutSession s : sessions) {
                if (s.date.equals(date) && s.workoutType.equals("rest")) {
                    hadRest = true;
                    break;
                }
            }
            if (hadRest) break;
            // Check if any workout was logged that day at all
            boolean hadAny = false;
            for (WorkoutSession s : sessions) {
                if (s.date.equals(date)) { hadAny = true; break; }
            }
            if (hadAny) count++;
            else break; // gap in logging — stop counting
        }
        return count;
    }

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

    private static String getDayLabel(int daysFromToday) {
        if (daysFromToday == 0) return "Today";
        if (daysFromToday == 1) return "Tomorrow";
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysFromToday);
        return new SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                .format(cal.getTime());
    }

    private static String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private static String daysAgo(int n) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -n);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(cal.getTime());
    }

    // ════════════════════════════════════════════════════════
    //  INNER DATA CLASSES (used only inside this engine)
    // ════════════════════════════════════════════════════════

    private static class PerformanceSnapshot {
        int    targetFrequency;
        String goal;
        int    completedThisWeek;
        float  avgIntensity;
        float  avgScore;
        float  scoreTrend;     // positive = improving
        String burnoutRisk;
        int    daysSinceRest;
    }

    private static class AdaptationStrategy {
        String type;           // "RECOVERY", "MAINTAIN", "PROGRESSIVE", "CATCH_UP"
        String planTitle;
        String description;
        String adaptationNote;
        int    intensityMod;   // -1, 0, or +1
        int    frequencyMod;   // -1, 0, or +1
    }
}