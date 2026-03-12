package com.example.aifitnessapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.ui.onboarding.OnboardingActivity;
import com.example.aifitnessapp.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No layout needed — this is just a router

        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Observe: does a user profile exist in the DB?
        viewModel.currentUser.observe(this, user -> {
            if (user != null) {
                // Profile exists → go to Dashboard
                startActivity(new Intent(this, DashboardActivity.class));
            } else {
                // No profile → go to Onboarding
                startActivity(new Intent(this, OnboardingActivity.class));
            }
            finish(); // Remove MainActivity from back stack
        });
    }
}