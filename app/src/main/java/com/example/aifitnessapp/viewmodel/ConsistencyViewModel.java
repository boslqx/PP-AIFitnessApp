package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.ConsistencyScore;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.engine.ConsistencyEngine;
import com.example.aifitnessapp.repository.ConsistencyScoreRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.List;

public class ConsistencyViewModel extends AndroidViewModel {

    private ConsistencyScoreRepository repository;

    public LiveData<UserProfile>             currentUser;
    public LiveData<List<ConsistencyScore>>  last7Scores;

    // Derived values computed once scores load
    public MutableLiveData<Float>   weeklyAvgScore  = new MutableLiveData<>(0f);
    public MutableLiveData<Integer> currentStreak   = new MutableLiveData<>(0);
    public MutableLiveData<String>  burnoutRisk     = new MutableLiveData<>("LOW");
    public MutableLiveData<String>  weekTrend       = new MutableLiveData<>("stable");
    public MutableLiveData<String>  coachMessage    = new MutableLiveData<>("");

    public ConsistencyViewModel(@NonNull Application application) {
        super(application);
        repository  = new ConsistencyScoreRepository(application);
        currentUser = FitAIDatabase.getInstance(application)
                .userProfileDao().getCurrentUser();
    }

    public void loadScores(int userId) {
        last7Scores = repository.getLast7Scores(userId);
    }

    /*
     * Called from the Activity after last7Scores LiveData delivers data.
     * Runs analysis on a background thread — results posted back via postValue().
     */
    public void analyzeScores(List<ConsistencyScore> scores) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            if (scores == null || scores.isEmpty()) {
                coachMessage.postValue(
                        "💡 No data yet — complete your first daily log to start tracking!");
                return;
            }

            // Weekly average
            float avg = ConsistencyEngine.calculateWeeklyScore(scores);
            weeklyAvgScore.postValue(avg);

            // Streak from most recent scores
            int streak = 0;
            for (ConsistencyScore s : scores) {
                if (s.score >= 50f) streak++;
                else break;
            }
            currentStreak.postValue(streak);

            // Week trend: compare first 3 vs last 3 days in the window
            if (scores.size() >= 4) {
                float recentAvg = (scores.get(0).score + scores.get(1).score) / 2f;
                float olderAvg  = (scores.get(scores.size() - 2).score
                        + scores.get(scores.size() - 1).score) / 2f;
                if      (recentAvg > olderAvg + 5)  weekTrend.postValue("improving ↑");
                else if (recentAvg < olderAvg - 5)  weekTrend.postValue("declining ↓");
                else                                weekTrend.postValue("stable →");
            }

            // Burnout risk (needs DailyLog too — use scores as proxy here)
            String risk = avg < 35 ? "HIGH" : avg < 55 ? "MEDIUM" : "LOW";
            burnoutRisk.postValue(risk);

            // Coach message
            coachMessage.postValue(buildCoachMessage(avg, streak, risk));
        });
    }

    private String buildCoachMessage(float avg, int streak, String risk) {
        if (risk.equals("HIGH"))
            return "⚠️ Your scores have been low this week. Consider a rest day and " +
                    "focus on sleep and hydration before pushing intensity.";
        if (streak >= 7)
            return "🔥 7-day streak! You've built real momentum. " +
                    "Consistency at this level leads to lasting change.";
        if (streak >= 3)
            return "✅ " + streak + "-day streak going! Keep showing up — " +
                    "streaks build habits, habits build results.";
        if (avg >= 75)
            return "💪 Excellent week! Your effort and recovery are well balanced.";
        if (avg >= 55)
            return "👍 Solid week. Focus on sleep quality to push your score higher.";
        return "💡 Every log counts. Even a short workout logged honestly " +
                "builds the data your AI needs to help you improve.";
    }

    public void computeTodayScore(int userId) {
        repository.computeAndSaveTodayScore(userId);
    }
}