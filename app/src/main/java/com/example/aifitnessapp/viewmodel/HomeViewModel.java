package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.repository.PlanRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends AndroidViewModel {

    private FitAIDatabase db;
    private PlanRepository planRepo;

    public LiveData<UserPreferences>      currentUser;
    public LiveData<PlannedWorkout>       todayPlan;
    public LiveData<List<PlannedWorkout>> weekPlan;
    public MutableLiveData<String>        coachMessage = new MutableLiveData<>("");
    public MutableLiveData<Boolean>       planReady    = new MutableLiveData<>(false);

    // Feed: past logged workout cards
    public MutableLiveData<List<FeedItem>> feedItems = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        db       = FitAIDatabase.getInstance(application);
        planRepo = new PlanRepository(application);
        currentUser = db.userPreferencesDao().getCurrentUser();
    }

    public void initPlan(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (!planRepo.planExistsForThisWeek(userId)) {
                planRepo.generateNextWeekPlan(userId);
            }
            planReady.postValue(true);
        });
    }

    public void loadPlan(int userId) {
        String weekStart = PlanEngine.getCurrentWeekStart();
        todayPlan = planRepo.getTodayPlan(userId);
        weekPlan  = planRepo.getWeekPlan(userId, weekStart);
    }

    public String getGreeting(String name) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String time = hour < 12 ? "Good morning"
                : hour < 17 ? "Good afternoon" : "Good evening";
        return time + ", " + name + " 👋";
    }

    public void loadWeekLabel(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int weekNum = db.plannedWorkoutDao().getLatestPlanWeek(userId);
            String today = PlanEngine.getTodayDayOfWeek();
            String[] days = {"MON","TUE","WED","THU","FRI","SAT","SUN"};
            int dayNum = 1;
            for (int i = 0; i < days.length; i++) {
                if (days[i].equals(today)) { dayNum = i + 1; break; }
            }
            coachMessage.postValue("Week " + weekNum + " · Day " + dayNum);
        });
    }

    public LiveData<WorkoutLog> getTodayLog(int userId) {
        String today = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return Transformations.map(
                db.workoutLogDao().getAllLogs(userId),
                logs -> {
                    if (logs == null) return null;
                    for (WorkoutLog log : logs) {
                        if (log.date.equals(today)) return log;
                    }
                    return null;
                });
    }

    /*
     * Loads past workout logs from the last 7 days (excluding today)
     * and pairs each with its PlannedWorkout for context.
     * Builds FeedItem list sorted newest first.
     */
    public void loadFeed(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // 7 days ago
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            String since = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            String today = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()).format(new Date());

            List<WorkoutLog> logs = db.workoutLogDao().getLogsSince(userId, since);
            List<FeedItem> items = new ArrayList<>();

            for (WorkoutLog log : logs) {
                // Skip today — it's shown in the today card
                if (log.date.equals(today)) continue;

                // Find matching planned workout
                String weekStart = getWeekStart(log.date);
                String dayOfWeek = getDayOfWeek(log.date);
                PlannedWorkout plan = db.plannedWorkoutDao()
                        .getTodayPlanSync(userId, weekStart, dayOfWeek);

                FeedItem item = new FeedItem();
                item.log          = log;
                item.plan         = plan;
                item.displayDate  = formatDisplayDate(log.date);
                item.activityType = plan != null ? plan.activityType : "GYM";
                item.sessionTitle = plan != null ? plan.sessionTitle : "Workout";
                item.isRestDay    = plan != null && plan.isRestDay;
                items.add(item);
            }

            // Reverse so newest is first
            java.util.Collections.reverse(items);
            feedItems.postValue(items);
        });
    }

    // ── Date helpers ──────────────────────────────────────────

    private String getWeekStart(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            int dow = c.get(Calendar.DAY_OF_WEEK);
            int sub = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
            c.add(Calendar.DAY_OF_YEAR, -sub);
            return sdf.format(c.getTime());
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
            return dow.format(date).toUpperCase(Locale.getDefault())
                    .substring(0, 3);
        } catch (Exception e) { return "MON"; }
    }

    private String formatDisplayDate(String dateStr) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat(
                    "EEE, MMM d", Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) { return dateStr; }
    }

    // ── FeedItem data class ───────────────────────────────────

    public static class FeedItem {
        public WorkoutLog     log;
        public PlannedWorkout plan;
        public String         displayDate;
        public String         activityType;
        public String         sessionTitle;
        public boolean        isRestDay;
    }
}