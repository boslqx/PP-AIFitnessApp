package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.model.UserProfile;
import com.example.aifitnessapp.engine.CalorieCalculator;
import com.example.aifitnessapp.repository.UserRepository;

public class OnboardingViewModel extends AndroidViewModel {

    private UserRepository userRepository;

    // MutableLiveData holds the in-progress profile as user fills each step
    // The UI observes this — no need to pass data manually between steps
    public MutableLiveData<UserProfile> draftProfile = new MutableLiveData<>(new UserProfile());
    public MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(null);

    public OnboardingViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    // Called on final step — calculate AI fields then save
    public void finalizeAndSave() {
        UserProfile profile = draftProfile.getValue();
        if (profile == null) return;

        // Run AI calculations before saving
        float bmr  = CalorieCalculator.calculateBMR(
                profile.weight, profile.height, profile.age, profile.gender);
        float tdee = CalorieCalculator.calculateTDEE(bmr, profile.activityLevel);

        profile.dailyCalorieTarget       = CalorieCalculator.calculateCalorieTarget(tdee, profile.fitnessGoal);
        profile.workoutFrequencyPerWeek  = CalorieCalculator.recommendWorkoutFrequency(
                profile.fitnessGoal, profile.activityLevel);
        profile.createdAt                = System.currentTimeMillis();

        userRepository.saveUserProfile(profile);
        saveSuccess.postValue(true);
    }
}