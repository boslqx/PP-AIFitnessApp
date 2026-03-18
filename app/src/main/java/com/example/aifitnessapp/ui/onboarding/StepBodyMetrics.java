package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.example.aifitnessapp.R;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepBodyMetrics implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private EditText etAge, etWeight, etHeight;

    public StepBodyMetrics(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Your body metrics"; }
    @Override public String getSubtitle() { return "Used to calculate your personalized calorie target."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context).inflate(R.layout.step_body_metrics, container, true);
        etAge    = view.findViewById(R.id.etAge);
        etWeight = view.findViewById(R.id.etWeight);
        etHeight = view.findViewById(R.id.etHeight);

        // Restore previously entered values
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft != null) {
            if (draft.age > 0)    etAge.setText(String.valueOf(draft.age));
            if (draft.weight > 0) etWeight.setText(String.valueOf(draft.weight));
            if (draft.height > 0) etHeight.setText(String.valueOf(draft.height));
        }
    }

    @Override
    public boolean validate() {
        String ageStr    = etAge.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();

        if (ageStr.isEmpty()) {
            etAge.setError("Required");
            return false;
        }
        if (weightStr.isEmpty()) {
            etWeight.setError("Required");
            return false;
        }
        if (heightStr.isEmpty()) {
            etHeight.setError("Required");
            return false;
        }

        int age = Integer.parseInt(ageStr);
        if (age < 10 || age > 100) {
            etAge.setError("Enter a valid age (10–100)");
            return false;
        }

        float weight = Float.parseFloat(weightStr);
        if (weight < 20 || weight > 300) {
            etWeight.setError("Enter a valid weight (20–300 kg)");
            return false;
        }

        float height = Float.parseFloat(heightStr);
        if (height < 100 || height > 250) {
            etHeight.setError("Enter a valid height (100–250 cm)");
            return false;
        }

        return true;
    }

    @Override
    public void saveToProfile() {
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        draft.age    = Integer.parseInt(etAge.getText().toString().trim());
        draft.weight = Float.parseFloat(etWeight.getText().toString().trim());
        draft.height = Float.parseFloat(etHeight.getText().toString().trim());

        viewModel.draftProfile.setValue(draft);
    }
}