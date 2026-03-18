package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepName implements OnboardingStep {

    private Context context;
    private OnboardingViewModel viewModel;
    private EditText etName;
    private RadioGroup rgGender;

    public StepName(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "What's your name?"; }
    @Override public String getSubtitle() { return "We'll use this to personalize your experience."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context).inflate(R.layout.step_name, container, true);
        etName   = view.findViewById(R.id.etName);
        rgGender = view.findViewById(R.id.rgGender);

        // Restore any previously entered value (e.g., if user goes Back then Next)
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.name != null) {
            etName.setText(draft.name);
        }
    }

    @Override
    public boolean validate() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Please enter your name");
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select your gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        draft.name = etName.getText().toString().trim();

        RadioButton selected = ((android.app.Activity) context)
                .findViewById(rgGender.getCheckedRadioButtonId());
        draft.gender = selected.getTag().toString(); // "male" or "female"

        viewModel.draftProfile.setValue(draft);
    }
}