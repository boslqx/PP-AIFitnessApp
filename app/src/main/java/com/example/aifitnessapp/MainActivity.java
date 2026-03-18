package com.example.aifitnessapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.ui.onboarding.OnboardingActivity;
import com.example.aifitnessapp.util.AppExecutors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppExecutors.getInstance().diskIO().execute(() -> {
            int count = FitAIDatabase.getInstance(getApplication())
                    .userPreferencesDao().getUserCount();

            runOnUiThread(() -> {
                if (count > 0) {
                    startActivity(new Intent(this, HomeActivity.class));
                } else {
                    startActivity(new Intent(this, OnboardingActivity.class));
                }
                finish();
            });
        });
    }
}