package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class StepBio implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private TextInputEditText etName, etAge, etWeight;

    public StepBio(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Almost done!"; }
    @Override public String getSubtitle() { return "Just a few basics so we can personalize your plan."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_bio, container, true);
        etName   = view.findViewById(R.id.etName);
        etAge    = view.findViewById(R.id.etAge);
        etWeight = view.findViewById(R.id.etWeight);

        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft != null) {
            if (draft.name != null)   etName.setText(draft.name);
            if (draft.age > 0)        etAge.setText(String.valueOf(draft.age));
            if (draft.weightKg > 0)   etWeight.setText(String.valueOf(draft.weightKg));
        }
    }

    @Override
    public boolean validate() {
        String name   = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String wtStr  = etWeight.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Required");
            return false;
        }
        if (ageStr.isEmpty()) {
            etAge.setError("Required");
            return false;
        }
        int age = Integer.parseInt(ageStr);
        if (age < 10 || age > 100) {
            etAge.setError("Enter a valid age (10–100)");
            return false;
        }
        if (wtStr.isEmpty()) {
            etWeight.setError("Required");
            return false;
        }
        float wt = Float.parseFloat(wtStr);
        if (wt < 20 || wt > 300) {
            etWeight.setError("Enter a valid weight (20–300 kg)");
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        draft.name     = etName.getText().toString().trim();
        draft.age      = Integer.parseInt(etAge.getText().toString().trim());
        draft.weightKg = Float.parseFloat(etWeight.getText().toString().trim());

        viewModel.draftProfile.setValue(draft);
    }
}