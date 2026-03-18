package com.example.aifitnessapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.ui.onboarding.OnboardingActivity;
import com.example.aifitnessapp.util.AppExecutors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check DB on background thread — route to correct screen
        AppExecutors.getInstance().diskIO().execute(() -> {
            int count = FitAIDatabase.getInstance(getApplication())
                    .userPreferencesDao().getUserCount();

            runOnUiThread(() -> {
                if (count > 0) {
                    // TODO: go to HomeActivity (Phase B)
                    // For now, loop back to show we saved successfully
                    startActivity(new Intent(this, OnboardingActivity.class));
                } else {
                    startActivity(new Intent(this, OnboardingActivity.class));
                }
                finish();
            });
        });
    }
}