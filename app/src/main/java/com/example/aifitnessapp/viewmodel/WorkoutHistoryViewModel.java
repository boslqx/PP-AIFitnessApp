package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.data.model.WorkoutSession;
import com.example.aifitnessapp.repository.WorkoutHistoryRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.List;

public class WorkoutHistoryViewModel extends AndroidViewModel {

    private WorkoutHistoryRepository repository;

    public LiveData<UserProfile>          currentUser;
    public LiveData<List<WorkoutSession>> recentSessions;

    // Summary stats shown in the header card
    public MutableLiveData<Integer> totalWorkouts    = new MutableLiveData<>(0);
    public MutableLiveData<Integer> totalMinutes     = new MutableLiveData<>(0);
    public MutableLiveData<Integer> totalCalories    = new MutableLiveData<>(0);
    public MutableLiveData<String>  favoriteType     = new MutableLiveData<>("—");
    public MutableLiveData<Float>   avgIntensity     = new MutableLiveData<>(0f);
    public MutableLiveData<String>  bestStreak       = new MutableLiveData<>("0 days");

    public WorkoutHistoryViewModel(@NonNull Application application) {
        super(application);
        repository  = new WorkoutHistoryRepository(application);
        currentUser = FitAIDatabase.getInstance(application)
                .userProfileDao().getCurrentUser();
    }

    public void loadSessions(int userId) {
        recentSessions = repository.getLast30Sessions(userId);
    }

    /*
     * Runs heavy stats computation on background thread.
     * Called from Activity after recentSessions delivers data.
     */
    public void computeStats(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            List<WorkoutSession> all = repository.getAllSessionsSync(userId);
            if (all == null || all.isEmpty()) return;

            int    sessions  = 0, minutes = 0, calories = 0;
            int    strength  = 0, cardio  = 0, flex     = 0;
            float  intensSum = 0;

            for (WorkoutSession s : all) {
                if (!s.completed) continue; // skip incomplete sessions
                sessions++;
                minutes  += s.durationMinutes;
                calories += s.caloriesBurned;
                intensSum += s.intensityLevel;

                switch (s.workoutType) {
                    case "strength":    strength++; break;
                    case "cardio":      cardio++;   break;
                    case "flexibility": flex++;     break;
                }
            }

            // Favourite type = whichever count is highest
            String fav = "strength";
            int favCount = strength;
            if (cardio > favCount)  { fav = "cardio";      favCount = cardio; }
            if (flex   > favCount)  { fav = "flexibility";                    }

            // Best streak = max consecutive days with a completed workout
            int streak = computeBestStreak(all);

            totalWorkouts.postValue(sessions);
            totalMinutes.postValue(minutes);
            totalCalories.postValue(calories);
            favoriteType.postValue(formatType(fav));
            avgIntensity.postValue(sessions > 0 ? intensSum / sessions : 0f);
            bestStreak.postValue(streak + " days");
        });
    }

    /*
     * Best streak: walk through sessions sorted by date,
     * count consecutive calendar days that had a completed workout.
     *
     * WHY THIS WORKS:
     * Sessions are ordered ASC by date from getAllSessionsSync.
     * We compare each session's date to the previous one:
     *   - same date   → same day, don't count twice
     *   - next day    → streak continues
     *   - gap > 1 day → streak resets to 1
     */
    private int computeBestStreak(List<WorkoutSession> sessions) {
        int best = 0, current = 0;
        String lastDate = null;

        for (WorkoutSession s : sessions) {
            if (!s.completed) continue;
            if (lastDate == null) {
                current = 1;
            } else if (s.date.equals(lastDate)) {
                // same day — don't increment
            } else if (isNextDay(lastDate, s.date)) {
                current++;
            } else {
                current = 1; // gap — reset
            }
            if (current > best) best = current;
            lastDate = s.date;
        }
        return best;
    }

    private boolean isNextDay(String prev, String next) {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date d1 = sdf.parse(prev);
            java.util.Date d2 = sdf.parse(next);
            long diff = d2.getTime() - d1.getTime();
            return diff == 86_400_000L; // exactly 1 day in ms
        } catch (Exception e) {
            return false;
        }
    }

    private String formatType(String type) {
        switch (type) {
            case "strength":    return "💪 Strength";
            case "cardio":      return "🏃 Cardio";
            case "flexibility": return "🧘 Flexibility";
            default:            return type;
        }
    }
}