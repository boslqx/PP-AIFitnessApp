package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.DailyLog;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.repository.ProgressRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProgressViewModel extends AndroidViewModel {

    public LiveData<UserProfile> currentUser;
    private ProgressRepository repository;

    // Chart datasets — each is a parallel list of values + labels
    public MutableLiveData<List<Float>>  weightValues  = new MutableLiveData<>();
    public MutableLiveData<List<String>> weightLabels  = new MutableLiveData<>();

    public MutableLiveData<List<Float>>  calorieValues = new MutableLiveData<>();
    public MutableLiveData<List<String>> calorieLabels = new MutableLiveData<>();

    public MutableLiveData<List<Float>>  sleepValues   = new MutableLiveData<>();
    public MutableLiveData<List<String>> sleepLabels   = new MutableLiveData<>();

    // Summary stats
    public MutableLiveData<String> weightSummary  = new MutableLiveData<>("");
    public MutableLiveData<String> calorieSummary = new MutableLiveData<>("");
    public MutableLiveData<String> sleepSummary   = new MutableLiveData<>("");

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        repository  = new ProgressRepository(application);
        currentUser = FitAIDatabase.getInstance(application)
                .userProfileDao().getCurrentUser();
    }

    public void loadProgress(int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            List<DailyLog> logs = repository.getAllLogsSync(userId);
            if (logs == null || logs.isEmpty()) return;

            // Build parallel value + label lists for each metric
            List<Float>  wVals = new ArrayList<>(), cVals = new ArrayList<>(),
                    sVals = new ArrayList<>();
            List<String> wLbls = new ArrayList<>(), cLbls = new ArrayList<>(),
                    sLbls = new ArrayList<>();

            for (DailyLog log : logs) {
                String label = shortDate(log.date); // "Mar 1"

                // Weight: only include days where user logged their weight
                if (log.weightKg > 0) {
                    wVals.add(log.weightKg);
                    wLbls.add(label);
                }

                // Calories: include all logged days
                if (log.caloriesConsumed > 0) {
                    cVals.add((float) log.caloriesConsumed);
                    cLbls.add(label);
                }

                // Sleep: include all days
                if (log.sleepHours > 0) {
                    sVals.add((float) log.sleepHours);
                    sLbls.add(label);
                }
            }

            weightValues.postValue(wVals);
            weightLabels.postValue(wLbls);
            calorieValues.postValue(cVals);
            calorieLabels.postValue(cLbls);
            sleepValues.postValue(sVals);
            sleepLabels.postValue(sLbls);

            // Build summary strings
            weightSummary.postValue(buildWeightSummary(wVals));
            calorieSummary.postValue(buildCalorieSummary(cVals));
            sleepSummary.postValue(buildSleepSummary(sVals));
        });
    }

    // ── Summary builders ──────────────────────────────────────

    private String buildWeightSummary(List<Float> vals) {
        if (vals.size() < 2) return "Log your weight daily to see trends.";
        float first = vals.get(0);
        float last  = vals.get(vals.size() - 1);
        float diff  = last - first;
        String direction = diff < 0 ? "lost" : "gained";
        return String.format("You've %s %.1f kg since you started tracking.", direction, Math.abs(diff));
    }

    private String buildCalorieSummary(List<Float> vals) {
        if (vals.isEmpty()) return "No calorie data yet.";
        float sum = 0;
        for (float v : vals) sum += v;
        float avg = sum / vals.size();
        return String.format("Average intake: %.0f kcal/day over %d days.", avg, vals.size());
    }

    private String buildSleepSummary(List<Float> vals) {
        if (vals.isEmpty()) return "No sleep data yet.";
        float sum = 0;
        for (float v : vals) sum += v;
        float avg = sum / vals.size();
        String quality = avg >= 8 ? "Excellent" : avg >= 7 ? "Good" : avg >= 6 ? "Fair" : "Poor";
        return String.format("%s average: %.1f hours/night.", quality, avg);
    }

    // "2025-03-11" → "Mar 11"
    private String shortDate(String dateStr) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return dateStr;
        }
    }
}