package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.aifitnessapp.repository.PlanRepository;

public class OnboardingViewModel extends AndroidViewModel {

    private FitAIDatabase db;

    public MutableLiveData<UserPreferences> draftProfile =
            new MutableLiveData<>(new UserPreferences());
    public MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(null);

    public OnboardingViewModel(@NonNull Application application) {
        super(application);
        db = FitAIDatabase.getInstance(application);
    }

    public void finalizeAndSave() {
        UserPreferences prefs = draftProfile.getValue();
        if (prefs == null) return;

        prefs.createdAt = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        prefs.updatedAt = prefs.createdAt;

        AppExecutors.getInstance().diskIO().execute(() -> {
            // 1. Save preferences
            db.userPreferencesDao().insert(prefs);

            // 2. Fetch the saved prefs (need the auto-generated id)
            UserPreferences saved = db.userPreferencesDao().getCurrentUserSync();

            // 3. Generate first 7-day plan immediately
            if (saved != null) {
                PlanRepository planRepo = new PlanRepository(getApplication());
                planRepo.generateFirstPlan(saved.id);
            }

            saveSuccess.postValue(true);
        });
    }
}
