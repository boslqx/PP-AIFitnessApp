package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.engine.CalorieCalculator;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepTargetWeight implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private EditText etTargetWeight;
    private TextView tvCalorieSummary;

    public StepTargetWeight(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Almost done!"; }
    @Override public String getSubtitle() { return "Set your target weight and see your personalized plan."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_target_weight, container, true);
        etTargetWeight  = view.findViewById(R.id.etTargetWeight);
        tvCalorieSummary = view.findViewById(R.id.tvCalorieSummary);

        // Restore previous value
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.targetWeight > 0) {
            etTargetWeight.setText(String.valueOf(draft.targetWeight));
        }

        // Show live AI calculation preview based on data from earlier steps
        showCalorieSummary(draft);
    }

    private void showCalorieSummary(UserProfile draft) {
        if (draft == null || draft.gender == null || draft.activityLevel == null
                || draft.fitnessGoal == null || draft.weight == 0) {
            tvCalorieSummary.setText("Complete earlier steps to see your plan.");
            return;
        }

        float bmr  = CalorieCalculator.calculateBMR(
                draft.weight, draft.height, draft.age, draft.gender);
        float tdee = CalorieCalculator.calculateTDEE(bmr, draft.activityLevel);
        int   cal  = CalorieCalculator.calculateCalorieTarget(tdee, draft.fitnessGoal);
        int[] mac  = CalorieCalculator.calculateMacros(cal, draft.fitnessGoal);

        // Display a human-readable summary of what the AI calculated
        String summary =
                "📊 Your Personalized Plan\n\n" +
                        "Daily Calories:  " + cal + " kcal\n" +
                        "Protein:  " + mac[0] + "g\n" +
                        "Carbs:    " + mac[1] + "g\n" +
                        "Fat:      " + mac[2] + "g\n\n" +
                        "Workouts/week:  " +
                        CalorieCalculator.recommendWorkoutFrequency(draft.fitnessGoal, draft.activityLevel);

        tvCalorieSummary.setText(summary);
    }

    @Override
    public boolean validate() {
        String targetStr = etTargetWeight.getText().toString().trim();
        if (targetStr.isEmpty()) {
            etTargetWeight.setError("Please enter your target weight");
            return false;
        }
        float target = Float.parseFloat(targetStr);
        if (target < 20 || target > 300) {
            etTargetWeight.setError("Enter a valid weight (20–300 kg)");
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft == null) return;
        draft.targetWeight = Float.parseFloat(etTargetWeight.getText().toString().trim());
        viewModel.draftProfile.setValue(draft);
    }
}