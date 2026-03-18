package com.example.aifitnessapp.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.util.AppExecutors;

public class UserRepository {

    private UserProfileDao userProfileDao;

    public UserRepository(Application application) {
        FitAIDatabase db = FitAIDatabase.getInstance(application);
        userProfileDao = db.userProfileDao();
    }

    public LiveData<UserProfile> getCurrentUser() {
        return userProfileDao.getCurrentUser();
        // LiveData query runs on a background thread automatically
    }

    public void saveUserProfile(UserProfile profile) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            userProfileDao.insert(profile);
        });
    }
}