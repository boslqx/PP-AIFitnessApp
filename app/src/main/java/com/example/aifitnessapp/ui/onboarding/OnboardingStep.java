package com.example.aifitnessapp.ui.onboarding;

import android.widget.FrameLayout;

// Contract that every onboarding step must follow
public interface OnboardingStep {
    String getTitle();
    String getSubtitle();
    void show(FrameLayout container);  // inflate and display this step's views
    boolean validate();                // return false if user input is invalid
    void saveToProfile();              // write input into viewModel.draftProfile
}