package com.example.aifitnessapp.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.MainActivity;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingViewModel viewModel;
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 6;

    private LinearProgressIndicator progressBar;
    private TextView tvStepIndicator, tvStepTitle, tvStepSubtitle;
    private Button btnBack, btnNext;
    private FrameLayout stepContainer;

    private OnboardingStep[] steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        progressBar     = findViewById(R.id.progressBar);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        tvStepTitle     = findViewById(R.id.tvStepTitle);
        tvStepSubtitle  = findViewById(R.id.tvStepSubtitle);
        btnBack         = findViewById(R.id.btnBack);
        btnNext         = findViewById(R.id.btnNext);
        stepContainer   = findViewById(R.id.stepContainer);

        steps = new OnboardingStep[]{
                new StepGoal(this, viewModel),          // Step 1: Fitness goal
                new StepActivities(this, viewModel),    // Step 2: Preferred activities
                new StepActivityDetails(this, viewModel),// Step 3: Activity-specific setup
                new StepFitnessLevel(this, viewModel),  // Step 4: Fitness level
                new StepCommitment(this, viewModel),    // Step 5: Days per week
                new StepBio(this, viewModel)            // Step 6: Name, age, weight
        };

        showStep(1);

        btnNext.setOnClickListener(v -> {
            if (!steps[currentStep - 1].validate()) return;
            steps[currentStep - 1].saveToProfile();

            if (currentStep < TOTAL_STEPS) {
                showStep(currentStep + 1);
            } else {
                viewModel.finalizeAndSave();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentStep > 1) showStep(currentStep - 1);
        });

        viewModel.saveSuccess.observe(this, success -> {
            if (success == null || !success) return;
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void showStep(int step) {
        currentStep = step;
        progressBar.setMax(TOTAL_STEPS);
        progressBar.setProgress(step);
        tvStepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);
        tvStepTitle.setText(steps[step - 1].getTitle());
        tvStepSubtitle.setText(steps[step - 1].getSubtitle());
        btnBack.setVisibility(step == 1 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(step == TOTAL_STEPS ? "Build My Plan 🚀" : "Next");
        steps[step - 1].show(stepContainer);
    }
}