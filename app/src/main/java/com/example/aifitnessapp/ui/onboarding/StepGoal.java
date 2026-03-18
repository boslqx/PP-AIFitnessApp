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

public class StepGoal implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;
    private RadioGroup rgGoal, rgTimeline;

    public StepGoal(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Your fitness goal"; }
    @Override public String getSubtitle() { return "This shapes your workout plan and calorie targets."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context).inflate(R.layout.step_goal, container, true);
        rgGoal     = view.findViewById(R.id.rgGoal);
        rgTimeline = view.findViewById(R.id.rgTimeline);

        // Restore previous selections
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft != null) {
            if ("fat_loss".equals(draft.fitnessGoal))    rgGoal.check(R.id.rbFatLoss);
            if ("muscle_gain".equals(draft.fitnessGoal)) rgGoal.check(R.id.rbMuscleGain);
            if ("endurance".equals(draft.fitnessGoal))   rgGoal.check(R.id.rbEndurance);

            if (draft.timelineMonths == 3) rgTimeline.check(R.id.rb3Months);
            if (draft.timelineMonths == 6) rgTimeline.check(R.id.rb6Months);
        }
    }

    @Override
    public boolean validate() {
        if (rgGoal.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select a fitness goal", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgTimeline.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select a timeline", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserProfile draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        RadioButton selectedGoal = ((android.app.Activity) context)
                .findViewById(rgGoal.getCheckedRadioButtonId());
        draft.fitnessGoal = selectedGoal.getTag().toString();

        RadioButton selectedTimeline = ((android.app.Activity) context)
                .findViewById(rgTimeline.getCheckedRadioButtonId());
        draft.timelineMonths = Integer.parseInt(selectedTimeline.getTag().toString());

        viewModel.draftProfile.setValue(draft);
    }
}