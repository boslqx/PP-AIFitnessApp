package com.example.aifitnessapp.ui.dashboard;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.viewmodel.DashboardViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;

    private TextView tvGreeting, tvDate, tvGoalSummary;
    private TextView tvConsistencyScore, tvStreakBadge, tvBurnoutRisk;
    private TextView tvCaloriesEaten, tvCaloriesTarget;
    private TextView tvProtein, tvCarbs, tvFat;
    private TextView tvAiInsight;
    private ProgressBar pbConsistency, pbCalories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        bindViews();
        observeData();
    }

    private void bindViews() {
        tvGreeting          = findViewById(R.id.tvGreeting);
        tvDate              = findViewById(R.id.tvDate);
        tvGoalSummary       = findViewById(R.id.tvGoalSummary);
        tvConsistencyScore  = findViewById(R.id.tvConsistencyScore);
        tvStreakBadge       = findViewById(R.id.tvStreakBadge);
        tvBurnoutRisk       = findViewById(R.id.tvBurnoutRisk);
        tvCaloriesEaten     = findViewById(R.id.tvCaloriesEaten);
        tvCaloriesTarget    = findViewById(R.id.tvCaloriesTarget);
        tvProtein           = findViewById(R.id.tvProtein);
        tvCarbs             = findViewById(R.id.tvCarbs);
        tvFat               = findViewById(R.id.tvFat);
        tvAiInsight         = findViewById(R.id.tvAiInsight);
        pbConsistency       = findViewById(R.id.pbConsistency);
        pbCalories          = findViewById(R.id.pbCalories);

        // Static content that doesn't need DB
        String today = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date());
        tvGreeting.setText(viewModel.getGreeting() + "! 👋");
        tvDate.setText(today);
    }

    private void observeData() {

        // Observe user profile — updates greeting and goal summary
        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;

            tvGreeting.setText(viewModel.getGreeting() + ", " + user.name + "! 👋");

            String goalLabel = formatGoal(user.fitnessGoal);
            tvGoalSummary.setText(goalLabel + " · " + user.dailyCalorieTarget + " kcal target");

            // Now that we have userId, compute AI insights
            viewModel.computeInsights(user.id);
        });

        // Observe today's nutrition log
        viewModel.todayLog.observe(this, log -> {
            if (log == null) {
                // No log yet today — show zeros
                tvCaloriesEaten.setText("0 kcal eaten");
                updateMacros(0, 0, 0);
                return;
            }
            tvCaloriesEaten.setText(log.caloriesConsumed + " kcal eaten");
            updateMacros((int) log.proteinGrams, (int) log.carbsGrams, (int) log.fatGrams);

            // Update calorie progress bar against target
            viewModel.currentUser.observe(this, user -> {
                if (user != null && user.dailyCalorieTarget > 0) {
                    tvCaloriesTarget.setText("/ " + user.dailyCalorieTarget + " target");
                    int pct = (int) ((log.caloriesConsumed / (float) user.dailyCalorieTarget) * 100);
                    pbCalories.setProgress(Math.min(pct, 100));
                }
            });
        });

        // Observe AI-computed consistency score
        viewModel.consistencyScore.observe(this, score -> {
            int rounded = Math.round(score);
            tvConsistencyScore.setText(String.valueOf(rounded));
            pbConsistency.setProgress(rounded);
        });

        // Observe burnout risk
        viewModel.burnoutRisk.observe(this, risk -> {
            String label = "Burnout risk: " + risk;
            tvBurnoutRisk.setText(label);
            // Change color based on risk level
            int color;
            switch (risk) {
                case "HIGH":   color = 0xFFD32F2F; break; // red
                case "MEDIUM": color = 0xFFF57F17; break; // amber
                default:       color = 0xFF757575; break; // grey
            }
            tvBurnoutRisk.setTextColor(color);
        });

        // Observe AI insight message
        viewModel.aiInsight.observe(this, insight -> {
            tvAiInsight.setText(insight);
        });
    }

    private void updateMacros(int protein, int carbs, int fat) {
        tvProtein.setText("Protein\n" + protein + "g");
        tvCarbs.setText("Carbs\n"   + carbs   + "g");
        tvFat.setText("Fat\n"       + fat      + "g");
    }

    private String formatGoal(String goal) {
        if (goal == null) return "Goal";
        switch (goal) {
            case "fat_loss":    return "Fat Loss";
            case "muscle_gain": return "Muscle Gain";
            case "endurance":   return "Endurance";
            default:            return goal;
        }
    }
}