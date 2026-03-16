package com.example.aifitnessapp.ui.dashboard;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.plan.WorkoutPlanActivity;
import com.example.aifitnessapp.viewmodel.DashboardViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;

public class DashboardActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;

    private TextView tvGreeting, tvDate, tvGoalSummary;
    private TextView tvConsistencyScore, tvStreakBadge, tvBurnoutRisk;
    private TextView tvCaloriesEaten, tvCaloriesTarget;
    private TextView tvProtein, tvCarbs, tvFat;
    private TextView tvAiInsight;
    private ProgressBar pbConsistency, pbCalories;
    Animator animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        bindViews();
        observeData();
    }

    private void bindViews() {
        tvGreeting         = findViewById(R.id.tvGreeting);
        tvDate             = findViewById(R.id.tvDate);
        tvGoalSummary      = findViewById(R.id.tvGoalSummary);
        tvConsistencyScore = findViewById(R.id.tvConsistencyScore);
        tvStreakBadge      = findViewById(R.id.tvStreakBadge);
        tvBurnoutRisk      = findViewById(R.id.tvBurnoutRisk);
        tvCaloriesEaten    = findViewById(R.id.tvCaloriesEaten);
        tvCaloriesTarget   = findViewById(R.id.tvCaloriesTarget);
        tvProtein          = findViewById(R.id.tvProtein);
        tvCarbs            = findViewById(R.id.tvCarbs);
        tvFat              = findViewById(R.id.tvFat);
        tvAiInsight        = findViewById(R.id.tvAiInsight);
        pbConsistency      = findViewById(R.id.pbConsistency);
        pbCalories         = findViewById(R.id.pbCalories);

        findViewById(R.id.btnLogToday).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.log.LogActivity.class)));

        findViewById(R.id.btnViewProgress).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.progress.ProgressActivity.class)));

        findViewById(R.id.btnWorkoutHistory).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.workout.WorkoutHistoryActivity.class)));

        findViewById(R.id.btnHabits).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.habit.HabitTrackerActivity.class)));

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.settings.SettingsActivity.class)));

        findViewById(R.id.btnMyPlan).setOnClickListener(v ->
                startActivity(new Intent(this, WorkoutPlanActivity.class)));

        findViewById(R.id.pbConsistency).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.consistency.ConsistencyActivity.class)));

        findViewById(R.id.tvConsistencyScore).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.aifitnessapp.ui.consistency.ConsistencyActivity.class)));

        String today = new SimpleDateFormat("EEEE, MMMM d",
                Locale.getDefault()).format(new Date());
        tvGreeting.setText(viewModel.getGreeting() + "! 👋");
        tvDate.setText(today);
    }

    private void observeData() {

        /*
         * EVERYTHING that needs userId lives inside this one observer.
         * currentUser delivers once when the DB returns the profile.
         * We then:
         *   1. Update the greeting + goal summary
         *   2. Initialize todayLog with the REAL userId (not hardcoded 1)
         *   3. Observe todayLog for nutrition data
         *   4. Kick off the AI insight computation
         */
        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;

            // 1. Greeting + goal
            tvGreeting.setText(viewModel.getGreeting() + ", " + user.name + "! 👋");
            String goalLabel = formatGoal(user.fitnessGoal);
            tvGoalSummary.setText(goalLabel + " · " + user.dailyCalorieTarget + " kcal target");

            // 2. Init todayLog with real userId
            viewModel.initTodayLog(user.id);

            // 3. Observe todayLog — this is now set up AFTER initTodayLog
            //    so we are guaranteed to observe the correct LiveData object
            viewModel.todayLog.observe(this, log -> {
                if (log == null) {
                    tvCaloriesEaten.setText("0 kcal eaten");
                    tvCaloriesTarget.setText("/ " + user.dailyCalorieTarget + " target");
                    pbCalories.setProgress(0);
                    updateMacros(0, 0, 0);
                    return;
                }
                tvCaloriesEaten.setText(log.caloriesConsumed + " kcal eaten");
                updateMacros((int) log.proteinGrams,
                        (int) log.carbsGrams,
                        (int) log.fatGrams);

                if (user.dailyCalorieTarget > 0) {
                    tvCaloriesTarget.setText("/ " + user.dailyCalorieTarget + " target");
                    int pct = (int) ((log.caloriesConsumed
                            / (float) user.dailyCalorieTarget) * 100);
                    ObjectAnimator calAnim = ObjectAnimator.ofInt(pbCalories, "progress", 0, Math.min(pct, 100));
                    calAnim.setDuration(800);
                    calAnim.setInterpolator(new DecelerateInterpolator());
                    calAnim.start();
                }
            });

            // 4. AI insights
            viewModel.computeInsights(user.id);
        });

        // These don't need userId so they can live outside
        viewModel.consistencyScore.observe(this, score -> {
            int rounded = Math.round(score);
            tvConsistencyScore.setText(String.valueOf(rounded));
            pbConsistency.setProgress(rounded);
            animation.setDuration(1000);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
        });

        viewModel.burnoutRisk.observe(this, risk -> {
            tvBurnoutRisk.setText("Burnout risk: " + risk);
            int color;
            switch (risk) {
                case "HIGH":   color = 0xFFD32F2F; break;
                case "MEDIUM": color = 0xFFF57F17; break;
                default:       color = 0xFF757575; break;
            }
            tvBurnoutRisk.setTextColor(color);
        });

        viewModel.aiInsight.observe(this, insight ->
                tvAiInsight.setText(insight));

        viewModel.streakCount.observe(this, streak -> {
            if (streak > 0) {
                tvStreakBadge.setVisibility(View.VISIBLE);
                tvStreakBadge.setText("🔥 " + streak + " day streak");
            } else {
                tvStreakBadge.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void updateMacros(int protein, int carbs, int fat) {
        tvProtein.setText("Protein\n" + protein + "g");
        tvCarbs.setText("Carbs\n"    + carbs    + "g");
        tvFat.setText("Fat\n"        + fat       + "g");
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