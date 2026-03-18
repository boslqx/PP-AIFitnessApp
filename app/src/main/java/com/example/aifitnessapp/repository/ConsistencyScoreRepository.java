package com.example.aifitnessapp.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConsistencyScoreRepository {

    private ConsistencyScoreDao scoreDao;
    private DailyLogDao         dailyLogDao;
    private WorkoutSessionDao   workoutDao;
    private HabitLogDao         habitDao;

    public ConsistencyScoreRepository(Application application) {
        FitAIDatabase db = FitAIDatabase.getInstance(application);
        scoreDao     = db.consistencyScoreDao();
        dailyLogDao  = db.dailyLogDao();
        workoutDao   = db.workoutSessionDao();
        habitDao     = db.habitLogDao();
    }

    public LiveData<List<ConsistencyScore>> getLast7Scores(int userId) {
        return scoreDao.getLast7Scores(userId);
    }

    /*
     * Called after user saves their daily log.
     * Pulls today's data, runs ConsistencyEngine, saves the score.
     *
     * HOW THE SCORE IS BUILT:
     *   completedTasks = workouts completed + habits completed today
     *   plannedTasks   = workouts planned  + total habits today
     *   intensityLevel = from today's workout session (default 3)
     *   currentStreak  = consecutive days where score >= 50
     *   sleepHours     = from today's DailyLog
     */
    public void computeAndSaveTodayScore(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            String today = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // 1. Get today's workout data
            int completedWorkouts = workoutDao.countCompletedSince(userId, today);
            int plannedWorkouts   = workoutDao.countPlannedSince(userId, today);

            // 2. Get today's habit data
            int completedHabits = habitDao.countCompletedHabitsOnDate(userId, today);
            int totalHabits     = habitDao.countTotalHabitsOnDate(userId, today);

            // 3. Combine: total completed vs total planned tasks
            int completed = completedWorkouts + completedHabits;
            int planned   = Math.max(plannedWorkouts + totalHabits, 1);
            // Math.max(..., 1) prevents division by zero if no habits logged

            // 4. Get intensity from today's workout (default 3 = moderate)
            List<WorkoutSession> todaySessions =
                    workoutDao.getAllSessionsSync(userId);
            int intensity = 3;
            for (WorkoutSession s : todaySessions) {
                if (s.date.equals(today) && s.completed) {
                    intensity = s.intensityLevel;
                    break;
                }
            }

            // 5. Get sleep from today's log
            List<DailyLog> allLogs = dailyLogDao.getAllLogsSync(userId);
            int sleepHours = 7; // default
            for (DailyLog log : allLogs) {
                if (log.date.equals(today)) {
                    sleepHours = log.sleepHours;
                    break;
                }
            }

            // 6. Calculate current streak (consecutive days score >= 50)
            List<ConsistencyScore> recent = scoreDao.getLast14ScoresSync(userId);
            int streak = calculateStreak(recent);

            // 7. Run the engine formula
            float score = ConsistencyEngine.calculateDailyScore(
                    completed, planned, intensity, streak, sleepHours);

            // 8. Determine trend vs yesterday
            String trend = "stable";
            if (!recent.isEmpty()) {
                float yesterday = recent.get(0).score;
                if      (score > yesterday + 5)  trend = "improving";
                else if (score < yesterday - 5)  trend = "declining";
            }

            // 9. Build and save the ConsistencyScore entity
            ConsistencyScore cs = new ConsistencyScore();
            cs.userId         = userId;
            cs.date           = today;
            cs.score          = score;
            cs.weekStreak     = streak;
            cs.completionRate = (float) completed / planned;
            cs.intensityFactor = intensity / 5f;
            cs.trend          = trend;

            scoreDao.insert(cs); // REPLACE strategy — safe to call multiple times per day
        });
    }

    /*
     * Streak = how many consecutive recent days had score >= 50
     * Scores are ordered DESC (newest first), so we walk from index 0 backward
     */
    private int calculateStreak(List<ConsistencyScore> scores) {
        int streak = 0;
        for (ConsistencyScore s : scores) {
            if (s.score >= 50f) streak++;
            else break; // streak broken
        }
        return streak;
    }
}