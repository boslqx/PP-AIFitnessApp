package com.example.aifitnessapp.engine;

import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlanEngine {

    // Day labels in order
    private static final String[] DAYS = {
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
    };

    // ─────────────────────────────────────────────────────────
    //  ENTRY POINT — called from repository on background thread
    //
    //  First week: recentLogs is empty, uses fresh start logic.
    //  Subsequent weeks: analyses logs to adapt intensity.
    // ─────────────────────────────────────────────────────────
    public static List<PlannedWorkout> generateWeekPlan(
            UserPreferences prefs,
            List<WorkoutLog> recentLogs,
            int planWeek,
            String weekStartDate) {

        // Step 1: Decide intensity for this week based on logs
        int baseIntensity = resolveIntensity(prefs, recentLogs);

        // Step 2: Decide which days are workout days
        boolean[] workoutDays = distributeWorkouts(prefs.daysPerWeek);

        // Step 3: Get the activity rotation for this user
        String[] activities = buildActivityRotation(prefs);

        // Step 4: Build 7 PlannedWorkout objects
        List<PlannedWorkout> plan = new ArrayList<>();
        int activityIndex = 0;

        for (int i = 0; i < 7; i++) {
            PlannedWorkout pw = new PlannedWorkout();
            pw.userId        = prefs.id;
            pw.planWeek      = planWeek;
            pw.dayOfWeek     = DAYS[i];
            pw.weekStartDate = weekStartDate;
            pw.createdAt     = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            if (workoutDays[i]) {
                String activity = activities[activityIndex % activities.length];
                activityIndex++;

                pw.activityType    = activity;
                pw.isRestDay       = false;
                pw.intensityLevel  = adjustIntensity(baseIntensity, i, planWeek);
                pw.durationMinutes = resolveDuration(activity, pw.intensityLevel);
                pw.sessionTitle    = buildTitle(activity, pw.intensityLevel, prefs.fitnessGoal);
                pw.sessionDetail   = buildDetail(activity, prefs);
                pw.coachNote       = buildCoachNote(
                        activity, pw.intensityLevel, recentLogs, planWeek);
            } else {
                pw.activityType    = "REST";
                pw.isRestDay       = true;
                pw.intensityLevel  = 0;
                pw.durationMinutes = 0;
                pw.sessionTitle    = "Rest Day";
                pw.sessionDetail   = "Recovery is part of the plan. Sleep well, stay hydrated.";
                pw.coachNote       = "Muscles grow during rest, not during the workout.";
            }

            plan.add(pw);
        }

        return plan;
    }

    // ─────────────────────────────────────────────────────────
    //  INTENSITY RESOLUTION
    //
    //  Week 1: based purely on fitness level
    //  Week 2+: adapts based on how user responded to last week
    //
    //  completed + felt easy (effort ≤ 2) → increase
    //  completed + felt hard (effort ≥ 4) → maintain
    //  skipped 2+ times                   → decrease
    //  consistent completion              → progress normally
    // ─────────────────────────────────────────────────────────
    public static int resolveIntensity(UserPreferences prefs, List<WorkoutLog> logs) {
        // Base intensity from fitness level
        int base;
        switch (prefs.fitnessLevel) {
            case "BEGINNER":     base = 2; break;
            case "INTERMEDIATE": base = 3; break;
            case "ADVANCED":     base = 4; break;
            default:             base = 2;
        }

        if (logs == null || logs.isEmpty()) return base;

        // Analyse last week's logs
        int completed = 0, skipped = 0;
        float totalEffort = 0;

        for (WorkoutLog log : logs) {
            switch (log.completionStatus) {
                case "COMPLETED": completed++; totalEffort += log.perceivedEffort; break;
                case "MODIFIED":  completed++; totalEffort += log.perceivedEffort; break;
                case "SKIPPED":   skipped++;   break;
            }
        }

        // Too many skips → drop intensity to reduce barrier
        if (skipped >= 2) return Math.max(1, base - 1);

        if (completed == 0) return base;

        float avgEffort = totalEffort / completed;

        // Felt too easy → step up
        if (avgEffort <= 2.0f) return Math.min(5, base + 1);

        // Felt very hard → hold
        if (avgEffort >= 4.5f) return base;

        // On track → gradual progression after week 2
        if (logs.size() >= 4) return Math.min(5, base + 1);

        return base;
    }

    // ─────────────────────────────────────────────────────────
    //  WORKOUT DISTRIBUTION
    //
    //  Spaces workout days as evenly as possible across the week.
    //  Always ensures at least 1 rest day between workout clusters.
    //
    //  Example: 4 days → [T, F, T, F, T, F, T] pattern
    //  where T = workout, F = rest (indices 0,2,4,6 or similar)
    // ─────────────────────────────────────────────────────────
    private static boolean[] distributeWorkouts(int count) {
        boolean[] days = new boolean[7];
        // Space workouts evenly using step = 7 / count
        for (int i = 0; i < count; i++) {
            int slot = Math.round(i * (7f / count));
            // If slot is taken, move forward
            while (slot < 7 && days[slot]) slot++;
            if (slot < 7) days[slot] = true;
        }
        return days;
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIVITY ROTATION
    //
    //  Builds an ordered list of activities to cycle through.
    //  Rules:
    //  - Mix activities based on fitness goal weight
    //  - If gym selected: gym sessions dominate muscle/fat goals
    //  - Running/cycling dominate endurance goal
    //  - Yoga/home fill lighter days
    //  - Bouldering/sports treated as full sessions
    // ─────────────────────────────────────────────────────────
    private static String[] buildActivityRotation(UserPreferences prefs) {
        List<String> rotation = new ArrayList<>();
        String[] selected = prefs.getActivitiesArray();

        // Build weighted pool based on goal
        for (int pass = 0; pass < 3; pass++) {
            for (String activity : selected) {
                boolean add = false;
                switch (prefs.fitnessGoal) {
                    case "BUILD_MUSCLE":
                        // Gym heavy, then bouldering, then others
                        add = (pass == 0 && activity.equals("GYM"))
                                || (pass == 1 && (activity.equals("GYM") || activity.equals("HOME")))
                                || (pass == 2);
                        break;
                    case "LOSE_FAT":
                        // Mix of cardio activities + gym
                        add = (pass == 0 && (activity.equals("RUNNING")
                                || activity.equals("CYCLING")
                                || activity.equals("SWIMMING")))
                                || (pass == 1 && (activity.equals("GYM") || activity.equals("HOME")))
                                || (pass == 2);
                        break;
                    case "ENDURANCE":
                        // Cardio dominant
                        add = (pass == 0 && (activity.equals("RUNNING")
                                || activity.equals("CYCLING")
                                || activity.equals("SWIMMING")))
                                || (pass == 1 && (activity.equals("BOULDERING")
                                || activity.equals("SPORTS")))
                                || (pass == 2);
                        break;
                    default: // GENERAL
                        add = true;
                        break;
                }
                if (add && !rotation.contains(activity)) rotation.add(activity);
            }
        }

        // Fallback: if rotation is empty somehow
        if (rotation.isEmpty()) rotation.add(selected[0]);

        return rotation.toArray(new String[0]);
    }

    // ─────────────────────────────────────────────────────────
    //  INTENSITY ADJUSTMENTS
    //
    //  Varies intensity across the week:
    //  - Start moderate, peak mid-week, taper Friday/Saturday
    //  - Week number adds progressive overload over time
    // ─────────────────────────────────────────────────────────
    private static int adjustIntensity(int base, int dayIndex, int weekNum) {
        // Day-of-week modifiers: MON=0, TUE=+1, WED=+1, THU=0, FRI=-1, SAT=-1, SUN=0
        int[] dayMod = {0, +1, +1, 0, -1, -1, 0};
        // Progressive overload: +1 every 2 weeks, capped at +2
        int weekMod = Math.min(2, (weekNum - 1) / 2);
        int adjusted = base + dayMod[dayIndex] + weekMod;
        return Math.max(1, Math.min(5, adjusted));
    }

    // ─────────────────────────────────────────────────────────
    //  DURATION
    //  Based on activity type and intensity level
    // ─────────────────────────────────────────────────────────
    private static int resolveDuration(String activity, int intensity) {
        int base;
        switch (activity) {
            case "GYM":        base = 60; break;
            case "RUNNING":    base = 30; break;
            case "BOULDERING": base = 75; break;
            case "CYCLING":    base = 45; break;
            case "SWIMMING":   base = 40; break;
            case "YOGA":       base = 40; break;
            case "HOME":       base = 35; break;
            case "SPORTS":     base = 60; break;
            default:           base = 40;
        }
        // intensity 1 = -10min, 3 = base, 5 = +10min
        return base + (intensity - 3) * 5;
    }

    // ─────────────────────────────────────────────────────────
    //  SESSION TITLE
    //  Short label shown in the home screen today card
    // ─────────────────────────────────────────────────────────
    private static String buildTitle(String activity, int intensity, String goal) {
        String intensityWord = intensity <= 2 ? "Light" : intensity == 3 ? "Moderate" : "Hard";

        switch (activity) {
            case "GYM":
                switch (goal) {
                    case "BUILD_MUSCLE": return intensityWord + " Gym — Strength";
                    case "LOSE_FAT":     return intensityWord + " Gym — Circuit";
                    default:             return intensityWord + " Gym Session";
                }
            case "RUNNING":    return intensityWord + " Run";
            case "BOULDERING": return "Bouldering Session";
            case "CYCLING":    return intensityWord + " Cycle";
            case "SWIMMING":   return intensityWord + " Swim";
            case "YOGA":       return "Yoga & Stretching";
            case "HOME":       return intensityWord + " Home Workout";
            case "SPORTS":     return "Sports Session";
            default:           return intensityWord + " Workout";
        }
    }

    // ─────────────────────────────────────────────────────────
    //  SESSION DETAIL
    //  What to actually DO — shown in the plan screen
    //  Gym details depend on equipment, others are general
    // ─────────────────────────────────────────────────────────
    private static String buildDetail(String activity, UserPreferences prefs) {
        switch (activity) {
            case "GYM":
                return buildGymDetail(prefs);
            case "RUNNING":
                return prefs.tracksRunningPace
                        ? "Easy pace run. Target: conversational effort. Track your pace."
                        : "Easy pace run. Go at a comfortable, conversational effort.";
            case "BOULDERING":
                return "Warm up on V0–V1 problems. Spend 60% time on your project grade. "
                        + "Cool down with easy problems and wrist stretches.";
            case "CYCLING":
                return "Steady pace ride. Keep cadence around 80–90 RPM. "
                        + "Focus on consistent effort, not speed.";
            case "SWIMMING":
                return "Warm up 200m easy. Main set: 4×100m at moderate pace. "
                        + "Cool down 100m easy. Focus on stroke technique.";
            case "YOGA":
                return "Full body flow. Focus on hip flexors, hamstrings, and thoracic spine. "
                        + "Hold each pose 30–60 seconds. Breathe deeply.";
            case "HOME":
                return buildHomeDetail(prefs.fitnessGoal);
            case "SPORTS":
                return prefs.sports != null && !prefs.sports.isEmpty()
                        ? "Play " + prefs.sports + ". Focus on movement quality and enjoyment."
                        : "Sports session. Play at your preferred intensity.";
            default:
                return "Complete your planned workout at the assigned intensity.";
        }
    }

    private static String buildGymDetail(UserPreferences prefs) {
        String equipment = prefs.gymEquipment != null ? prefs.gymEquipment : "FULL_GYM";

        switch (prefs.fitnessGoal) {
            case "BUILD_MUSCLE":
                switch (equipment) {
                    case "DUMBBELLS":
                        return "3×10 Dumbbell press · 3×12 DB rows · 3×10 DB shoulder press "
                                + "· 3×15 DB lunges · 3×12 DB bicep curl · Core 3×20";
                    case "BARBELL_RACK":
                        return "4×5 Squat · 4×5 Bench press · 3×8 Barbell row "
                                + "· 3×8 Overhead press · 3×10 RDL";
                    case "MACHINES":
                        return "3×12 Chest press machine · 3×12 Lat pulldown · 3×12 Leg press "
                                + "· 3×15 Shoulder press · 3×15 Seated row";
                    default: // FULL_GYM
                        return "4×5 Squat · 4×5 Bench press · 3×8 Barbell row "
                                + "· 3×10 Incline DB press · 3×12 Cable row · Core 3×20";
                }
            case "LOSE_FAT":
                switch (equipment) {
                    case "DUMBBELLS":
                        return "Circuit ×3: DB thrusters ×15 · Renegade rows ×10 "
                                + "· DB step-ups ×12 · Plank 45s · Rest 60s";
                    default:
                        return "Circuit ×3: Barbell complex (clean+press+squat) ×8 "
                                + "· Treadmill intervals 30s on/30s off ×8 · Core 3×20";
                }
            default: // ENDURANCE / GENERAL
                return "Full body: 3×15 each — squats, push-ups, rows, shoulder press, "
                        + "lunges. Keep rest under 60s. Heart rate moderate throughout.";
        }
    }

    private static String buildHomeDetail(String goal) {
        switch (goal) {
            case "BUILD_MUSCLE":
                return "4×10 Push-ups · 4×12 Glute bridges · 3×10 Pike push-ups "
                        + "· 3×15 Reverse lunges · 3×20 Superman holds";
            case "LOSE_FAT":
                return "HIIT circuit ×4: Jump squats ×15 · Push-ups ×10 · High knees 30s "
                        + "· Plank 30s · Rest 45s between rounds";
            default:
                return "3×12: Squats · Push-ups · Hip hinges · Mountain climbers · "
                        + "Plank hold 30s. Focus on form over speed.";
        }
    }

    // ─────────────────────────────────────────────────────────
    //  COACH NOTE
    //  The "why" shown below each day in the plan screen
    // ─────────────────────────────────────────────────────────
    private static String buildCoachNote(String activity, int intensity,
                                         List<WorkoutLog> recentLogs, int weekNum) {
        if (recentLogs == null || recentLogs.isEmpty()) {
            // First week — motivational
            return "Week 1 — let's establish your baseline. Focus on form, not speed.";
        }

        // Count skips
        int skipped = 0;
        for (WorkoutLog log : recentLogs) {
            if ("SKIPPED".equals(log.completionStatus)) skipped++;
        }

        if (skipped >= 2) {
            return "Intensity eased this week — consistency matters more than volume. "
                    + "Show up, even if the session feels easy.";
        }

        if (weekNum >= 3 && intensity >= 4) {
            return "Week " + weekNum + " — you've earned this progression. "
                    + "Push hard, but listen to your body.";
        }

        return "Week " + weekNum + " · "
                + (intensity <= 2 ? "Recovery pace — quality over quantity."
                :  intensity == 3 ? "Solid working session. Maintain good form throughout."
                :                   "High effort today. Rest well tonight.");
    }

    // ─────────────────────────────────────────────────────────
    //  DATE UTILITIES
    // ─────────────────────────────────────────────────────────

    // Returns the ISO date string of the most recent Monday
    public static String getCurrentWeekStart() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        // Calendar.MONDAY = 2
        int daysToSubtract = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    // Returns "MON", "TUE", etc. for today
    public static String getTodayDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        switch (dow) {
            case Calendar.MONDAY:    return "MON";
            case Calendar.TUESDAY:  return "TUE";
            case Calendar.WEDNESDAY:return "WED";
            case Calendar.THURSDAY: return "THU";
            case Calendar.FRIDAY:   return "FRI";
            case Calendar.SATURDAY: return "SAT";
            case Calendar.SUNDAY:   return "SUN";
            default:                return "MON";
        }
    }
}