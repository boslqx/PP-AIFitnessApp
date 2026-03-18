package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardViewModel extends AndroidViewModel {

    // LiveData the UI observes — auto-updates when DB changes
    public LiveData<UserProfile> currentUser;
    public LiveData<DailyLog> todayLog;
    public MutableLiveData<Float> consistencyScore = new MutableLiveData<>(0f);
    public MutableLiveData<String> aiInsight = new MutableLiveData<>("Loading your insights...");
    public MutableLiveData<String> burnoutRisk = new MutableLiveData<>("LOW");
    private FitAIDatabase db;
    public MutableLiveData<Integer> streakCount = new MutableLiveData<>(0);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        db = FitAIDatabase.getInstance(application);

        // These are LiveData — Room delivers updates automatically on any DB change
        currentUser = db.userProfileDao().getCurrentUser();
        // todayLog    = db.dailyLogDao().getLogByDate(1, getTodayDate());
        // Note: userId=1 hardcoded for Phase 1 (single user)
    }

    public void initTodayLog(int userId) {
        todayLog = db.dailyLogDao().getLogByDate(userId, getTodayDate());
    }

    // Called once after user data loads, to compute AI insights
    public void computeInsights(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            // Fetch raw data for analysis
            List<com.example.aifitnessapp.data.model.ConsistencyScore> last7Scores =
                    db.consistencyScoreDao().getLast7ScoresSync(userId);

            List<DailyLog> last7Logs =
                    db.dailyLogDao().getAllLogsSync(userId);

            // Calculate weekly score
            float score = ConsistencyEngine.calculateWeeklyScore(last7Scores);

            // Generate insight message
            String insight = generateInsight(score, last7Logs, last7Scores);

            // Detect burnout risk
            String risk = ConsistencyEngine.detectBurnoutRisk(last7Logs, last7Scores);

            // postValue() — safe to call from background thread
            // It switches to main thread automatically for LiveData observers
            consistencyScore.postValue(score);
            aiInsight.postValue(insight);
            burnoutRisk.postValue(risk);

            int streak = 0;
            for (ConsistencyScore s : last7Scores) {
                if (s.score >= 50f) streak++;
                else break;
            }
            streakCount.postValue(streak);
        });
    }

    private String generateInsight(float score,
                                   List<DailyLog> logs,
                                   List<com.example.aifitnessapp.data.model.ConsistencyScore> scores) {

        if (scores.isEmpty()) return "💡 Start logging to get AI insights!";

        String risk = ConsistencyEngine.detectBurnoutRisk(logs, scores);
        if (risk.equals("HIGH")) return "⚠️ High burnout risk. Consider a rest day today.";
        if (score >= 80) return "🔥 Outstanding week! You're on fire. Keep it up!";
        if (score >= 60) return "✅ Good consistency! Small improvements compound fast.";
        if (score >= 40) return "💪 Tough week. Even a 10-min walk counts toward your goal.";
        return "💡 Let's get back on track. Start with one small win today.";
    }

    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public String getGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }
}