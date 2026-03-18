package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepActivityLevel implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private RadioGroup rgActivityLevel;

    public StepActivityLevel(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Activity level"; }
    @Override public String getSubtitle() { return "How active are you on a typical week?"; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_activity_level, container, true);
        rgActivityLevel = view.findViewById(R.id.rgActivityLevel);

        // Restore previous selection
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.activityLevel != null) {
            restoreSelection(draft.activityLevel);
        }
    }

    private void restoreSelection(String activityLevel) {
        // Map saved value back to radio button id
        int[] ids = {
                R.id.rbSedentary, R.id.rbLight,
                R.id.rbModerate,  R.id.rbActive, R.id.rbVeryActive
        };
        String[] values = { "sedentary", "light", "moderate", "active", "very_active" };
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(activityLevel)) {
                rgActivityLevel.check(ids[i]);
                break;
            }
        }
    }

    @Override
    public boolean validate() {
        if (rgActivityLevel.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select your activity level", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        RadioButton selected = ((android.app.Activity) context)
                .findViewById(rgActivityLevel.getCheckedRadioButtonId());
        draft.activityLevel = selected.getTag().toString();

        viewModel.draftProfile.setValue(draft);
    }
}