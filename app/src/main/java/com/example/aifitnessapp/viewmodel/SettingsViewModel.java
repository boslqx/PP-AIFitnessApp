package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.engine.CalorieCalculator;
import com.example.aifitnessapp.repository.UserRepository;

public class SettingsViewModel extends AndroidViewModel {

    private UserRepository repository;

    public LiveData<UserProfile> currentUser;
    public MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(null);

    /*
     * Live preview of recalculated targets — updates as user changes fields.
     * Shown in the green card before they hit Save.
     */
    public MutableLiveData<String> targetPreview = new MutableLiveData<>("");

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        repository  = new UserRepository(application);
        currentUser = repository.getCurrentUser();
    }

    /*
     * Called whenever any field changes.
     * Recalculates targets and posts a preview string — does NOT save yet.
     */
    public void previewTargets(float weight, float height, int age, String gender,
                               String activityLevel, String goal) {
        if (weight <= 0 || height <= 0 || age <= 0
                || gender == null || activityLevel == null || goal == null) {
            targetPreview.postValue("Fill in all fields to preview targets.");
            return;
        }

        float bmr  = CalorieCalculator.calculateBMR(weight, height, age, gender);
        float tdee = CalorieCalculator.calculateTDEE(bmr, activityLevel);
        int   cal  = CalorieCalculator.calculateCalorieTarget(tdee, goal);
        int[] mac  = CalorieCalculator.calculateMacros(cal, goal);
        int   freq = CalorieCalculator.recommendWorkoutFrequency(goal, activityLevel);

        String preview =
                "Daily Calories:  " + cal  + " kcal\n" +
                        "Protein:         " + mac[0] + "g\n"   +
                        "Carbs:           " + mac[1] + "g\n"   +
                        "Fat:             " + mac[2] + "g\n\n" +
                        "Workouts/week:   " + freq;

        targetPreview.postValue(preview);
    }

    /*
     * Called when user taps Save.
     * Recalculates AI fields, updates the profile, persists to DB.
     * Preserves the original profile id and createdAt timestamp.
     */
    public void saveSettings(UserProfile updated) {
        float bmr  = CalorieCalculator.calculateBMR(
                updated.weight, updated.height, updated.age, updated.gender);
        float tdee = CalorieCalculator.calculateTDEE(bmr, updated.activityLevel);

        updated.dailyCalorieTarget      = CalorieCalculator.calculateCalorieTarget(tdee, updated.fitnessGoal);
        updated.workoutFrequencyPerWeek = CalorieCalculator.recommendWorkoutFrequency(
                updated.fitnessGoal, updated.activityLevel);

        repository.saveUserProfile(updated);
        saveSuccess.postValue(true);
    }
}