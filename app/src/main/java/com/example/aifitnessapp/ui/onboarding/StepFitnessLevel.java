package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepFitnessLevel implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private RadioGroup rgLevel;

    public StepFitnessLevel(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Your fitness level"; }
    @Override public String getSubtitle() { return "Be honest — this sets your starting intensity."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_fitness_level, container, true);
        rgLevel = view.findViewById(R.id.rgFitnessLevel);

        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.fitnessLevel != null) {
            switch (draft.fitnessLevel) {
                case "BEGINNER":     rgLevel.check(R.id.rbBeginner);     break;
                case "INTERMEDIATE": rgLevel.check(R.id.rbIntermediate); break;
                case "ADVANCED":     rgLevel.check(R.id.rbAdvanced);     break;
            }
        }
    }

    @Override
    public boolean validate() {
        if (rgLevel.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select your fitness level", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        int id = rgLevel.getCheckedRadioButtonId();
        if (id == R.id.rbBeginner)     draft.fitnessLevel = "BEGINNER";
        if (id == R.id.rbIntermediate) draft.fitnessLevel = "INTERMEDIATE";
        if (id == R.id.rbAdvanced)     draft.fitnessLevel = "ADVANCED";

        viewModel.draftProfile.setValue(draft);
    }
}