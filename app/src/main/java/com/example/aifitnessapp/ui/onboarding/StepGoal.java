package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepGoal implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private RadioGroup rgGoal;

    public StepGoal(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "What's your fitness goal?"; }
    @Override public String getSubtitle() { return "This shapes everything — your plan, intensity, and progression."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_goal, container, true);
        rgGoal = view.findViewById(R.id.rgGoal);

        // Restore previous selection
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.fitnessGoal != null) {
            switch (draft.fitnessGoal) {
                case "LOSE_FAT":      rgGoal.check(R.id.rbLoseFat);      break;
                case "BUILD_MUSCLE":  rgGoal.check(R.id.rbBuildMuscle);  break;
                case "ENDURANCE":     rgGoal.check(R.id.rbEndurance);    break;
                case "GENERAL":       rgGoal.check(R.id.rbGeneral);      break;
            }
        }
    }

    @Override
    public boolean validate() {
        if (rgGoal.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select a goal", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        int id = rgGoal.getCheckedRadioButtonId();
        if (id == R.id.rbLoseFat)     draft.fitnessGoal = "LOSE_FAT";
        if (id == R.id.rbBuildMuscle) draft.fitnessGoal = "BUILD_MUSCLE";
        if (id == R.id.rbEndurance)   draft.fitnessGoal = "ENDURANCE";
        if (id == R.id.rbGeneral)     draft.fitnessGoal = "GENERAL";

        viewModel.draftProfile.setValue(draft);
    }
}