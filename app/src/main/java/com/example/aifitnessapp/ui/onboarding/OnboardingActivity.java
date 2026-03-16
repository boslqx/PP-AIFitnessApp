package com.example.aifitnessapp.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.MainActivity;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingViewModel viewModel;
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 5;

    private ProgressBar progressBar;
    private TextView tvStepIndicator, tvStepTitle, tvStepSubtitle;
    private Button btnBack, btnNext;

    // Step fragment/view references (we'll build these next)
    private OnboardingStep[] steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Get the ViewModel — survives rotation
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        // Bind views
        progressBar     = findViewById(R.id.progressBar);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        tvStepTitle     = findViewById(R.id.tvStepTitle);
        tvStepSubtitle  = findViewById(R.id.tvStepSubtitle);
        btnBack         = findViewById(R.id.btnBack);
        btnNext         = findViewById(R.id.btnNext);

        // Initialize step views
        steps = new OnboardingStep[]{
                new StepName(this, viewModel),         // Step 1: Name + Gender
                new StepBodyMetrics(this, viewModel),  // Step 2: Age + Weight + Height
                new StepActivityLevel(this, viewModel),// Step 3: Activity Level
                new StepGoal(this, viewModel),         // Step 4: Goal + Timeline
                new StepTargetWeight(this, viewModel)  // Step 5: Target Weight + Summary
        };

        showStep(1);

        btnNext.setOnClickListener(v -> {
            // Validate current step before advancing
            if (!steps[currentStep - 1].validate()) return;

            // Save current step's data into draftProfile
            steps[currentStep - 1].saveToProfile();

            if (currentStep < TOTAL_STEPS) {
                showStep(currentStep + 1);
            } else {
                // Final step — calculate AI fields and save
                viewModel.finalizeAndSave();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentStep > 1) showStep(currentStep - 1);
        });

        // Observe save success — navigate to dashboard when done
        viewModel.saveSuccess.observe(this, success -> {
            if (success == null || !success) return; {
                startActivity(new Intent(this, MainActivity.class));
                finish(); // prevent going back to onboarding
            }
        });
    }

    private void showStep(int step) {
        currentStep = step;

        // Update header
        progressBar.setProgress(step);
        tvStepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);
        tvStepTitle.setText(steps[step - 1].getTitle());
        tvStepSubtitle.setText(steps[step - 1].getSubtitle());

        // Show/hide Back button
        btnBack.setVisibility(step == 1 ? View.INVISIBLE : View.VISIBLE);

        // Change Next button label on final step
        btnNext.setText(step == TOTAL_STEPS ? "Let's Go! 🚀" : "Next");

        // Swap the content view
        steps[step - 1].show(findViewById(R.id.stepContainer));
    }
}