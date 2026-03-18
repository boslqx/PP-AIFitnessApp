package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepCommitment implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private RadioGroup rgDays;

    public StepCommitment(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "How many days per week?"; }
    @Override public String getSubtitle() { return "Pick a realistic number — the AI will build around it."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_commitment, container, true);
        rgDays = view.findViewById(R.id.rgDaysPerWeek);

        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.daysPerWeek > 0) {
            switch (draft.daysPerWeek) {
                case 2: rgDays.check(R.id.rb2Days); break;
                case 3: rgDays.check(R.id.rb3Days); break;
                case 4: rgDays.check(R.id.rb4Days); break;
                case 5: rgDays.check(R.id.rb5Days); break;
                case 6: rgDays.check(R.id.rb6Days); break;
            }
        }
    }

    @Override
    public boolean validate() {
        if (rgDays.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select how many days", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        int id = rgDays.getCheckedRadioButtonId();
        if (id == R.id.rb2Days) draft.daysPerWeek = 2;
        if (id == R.id.rb3Days) draft.daysPerWeek = 3;
        if (id == R.id.rb4Days) draft.daysPerWeek = 4;
        if (id == R.id.rb5Days) draft.daysPerWeek = 5;
        if (id == R.id.rb6Days) draft.daysPerWeek = 6;

        viewModel.draftProfile.setValue(draft);
    }
}