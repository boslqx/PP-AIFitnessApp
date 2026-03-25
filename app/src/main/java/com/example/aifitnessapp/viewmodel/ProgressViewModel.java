package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProgressViewModel extends AndroidViewModel {

    private FitAIDatabase db;
    public LiveData<UserPreferences> currentUser;

    // ── Stats LiveData ─────────────────────────────────────────
    public MutableLiveData<Integer> totalWorkouts = new MutableLiveData<>(0);
    public MutableLiveData<Integer> completionPct = new MutableLiveData<>(0);
    public MutableLiveData<Integer> currentStreak = new MutableLiveData<>(0);
    public MutableLiveData<Integer> totalSkipped  = new MutableLiveData<>(0);
    public MutableLiveData<String>  memberSince   = new MutableLiveData<>("");

    // History list — each item is one display row
    public MutableLiveData<List<HistoryRow>> historyRows = new MutableLiveData<>();

    // Activity breakdown — activityType → count
    public MutableLiveData<Map<String, Integer>> activityBreakdown = new MutableLiveData<>();

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        db = FitAIDatabase.getInstance(application);
        currentUser = db.userPreferencesDao().getCurrentUser();
    }

    public void loadProgress(int userId) {
        isLoading.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {

            String earliest = "2020-01-01";
            List<WorkoutLog> allLogsSince = db.workoutLogDao()
                    .getLogsSince(userId, earliest);

            int total      = 0;
            int skipped    = 0;
            int tempStreak = 0;
            int maxStreak  = 0;

            Map<String, Integer> breakdown = new HashMap<>();
            List<HistoryRow>     rows      = new ArrayList<>();

            for (WorkoutLog log : allLogsSince) {

                // Find the matching planned workout for context
                PlannedWorkout plan = db.plannedWorkoutDao()
                        .getTodayPlanSync(userId,
                                getPlanWeekStart(log.date),
                                getDayOfWeek(log.date));

                String  activityType = plan != null ? plan.activityType : "—";
                String  sessionTitle = plan != null ? plan.sessionTitle  : "Workout";
                boolean isRest       = plan != null && plan.isRestDay;

                if (!isRest) {
                    if ("COMPLETED".equals(log.completionStatus)
                            || "MODIFIED".equals(log.completionStatus)) {
                        total++;
                        tempStreak++;
                        if (tempStreak > maxStreak) maxStreak = tempStreak;
                    } else if ("SKIPPED".equals(log.completionStatus)) {
                        skipped++;
                        tempStreak = 0;
                    }

                    if (!"—".equals(activityType)) {
                        breakdown.put(activityType,
                                breakdown.getOrDefault(activityType, 0) + 1);
                    }
                }

                // ── Build history row ─────────────────────────
                // photoPath is passed straight from WorkoutLog.
                // The Activity will use it to load the thumbnail.
                // If null or empty, the Activity shows the emoji icon instead.
                HistoryRow row         = new HistoryRow();
                row.date               = formatDisplayDate(log.date);
                row.statusIcon         = statusIcon(log.completionStatus);
                row.activityType       = activityType;
                row.sessionTitle       = sessionTitle;
                row.completionStatus   = log.completionStatus;
                row.perceivedEffort    = log.perceivedEffort;
                row.isRestDay          = isRest;
                row.photoPath          = log.photoPath; // ← NEW: carry photo path through
                rows.add(row);
            }

            int totalPlanned = total + skipped;
            int pct = totalPlanned > 0
                    ? Math.round((float) total / totalPlanned * 100) : 0;

            // Member since date
            UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
            String since = "";
            if (prefs != null && prefs.createdAt != null) {
                try {
                    SimpleDateFormat in  = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat out = new SimpleDateFormat(
                            "MMM d, yyyy", Locale.getDefault());
                    since = out.format(in.parse(prefs.createdAt));
                } catch (Exception e) {
                    since = prefs.createdAt;
                }
            }

            totalWorkouts.postValue(total);
            completionPct.postValue(pct);
            currentStreak.postValue(maxStreak);
            totalSkipped.postValue(skipped);
            memberSince.postValue(since);
            historyRows.postValue(rows);
            activityBreakdown.postValue(breakdown);
            isLoading.postValue(false);
        });
    }

    // ── Date helpers ──────────────────────────────────────────

    private String getPlanWeekStart(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int sub = (dow == java.util.Calendar.SUNDAY) ? 6
                    : dow - java.util.Calendar.MONDAY;
            cal.add(java.util.Calendar.DAY_OF_YEAR, -sub);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return PlanEngine.getCurrentWeekStart();
        }
    }

    private String getDayOfWeek(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            SimpleDateFormat dow = new SimpleDateFormat(
                    "EEE", Locale.getDefault());
            return dow.format(date).toUpperCase(Locale.getDefault()).substring(0, 3);
        } catch (Exception e) {
            return "MON";
        }
    }

    private String formatDisplayDate(String dateStr) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat(
                    "EEE, MMM d", Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String statusIcon(String status) {
        switch (status) {
            case "COMPLETED": return "✅";
            case "MODIFIED":  return "✏️";
            case "SKIPPED":   return "⏭️";
            default:          return "—";
        }
    }

    // ── HistoryRow data class ─────────────────────────────────
    /*
     * This is a plain data container — no logic, just fields.
     * It exists so the ViewModel can hand the Activity everything
     * it needs in one object, instead of the Activity juggling
     * multiple parallel lists.
     *
     * photoPath is nullable — null means no photo was taken.
     * The Activity checks for null before trying to load the image.
     */
    public static class HistoryRow {
        public String  date;
        public String  statusIcon;
        public String  activityType;
        public String  sessionTitle;
        public String  completionStatus;
        public int     perceivedEffort;
        public boolean isRestDay;
        public String  photoPath;    // ← NEW: null if no photo taken
    }
}