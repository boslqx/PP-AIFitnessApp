// ui/onboarding/OnboardingStep.java
package com.example.aifitnessapp.ui.onboarding;

import android.widget.FrameLayout;

public interface OnboardingStep {
    String getTitle();
    String getSubtitle();
    void show(FrameLayout container);
    boolean validate();
    void saveToProfile();
}