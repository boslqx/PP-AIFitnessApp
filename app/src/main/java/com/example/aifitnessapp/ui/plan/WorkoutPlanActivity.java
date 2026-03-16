package com.example.aifitnessapp.ui.plan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.WorkoutPlan;
import com.example.aifitnessapp.data.model.WorkoutPlanDay;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.WorkoutPlanViewModel;

public class WorkoutPlanActivity extends AppCompatActivity {

    private WorkoutPlanViewModel viewModel;
    private LinearLayout planDayContainer;
    private TextView tvPlanTitle, tvPlanDescription, tvAdaptationNote;
    private TextView tvPlanTarget, tvPlanScoreBasis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_plan);

        viewModel = new ViewModelProvider(this).get(WorkoutPlanViewModel.class);

        bindViews();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            viewModel.generatePlan(user.id);
        });

        viewModel.workoutPlan.observe(this, plan -> {
            if (plan == null) return;
            renderPlan(plan);
        });

        findViewById(R.id.btnBackFromPlan).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void bindViews() {
        planDayContainer   = findViewById(R.id.planDayContainer);
        tvPlanTitle        = findViewById(R.id.tvPlanTitle);
        tvPlanDescription  = findViewById(R.id.tvPlanDescription);
        tvAdaptationNote   = findViewById(R.id.tvAdaptationNote);
        tvPlanTarget       = findViewById(R.id.tvPlanTarget);
        tvPlanScoreBasis   = findViewById(R.id.tvPlanScoreBasis);
    }

    private void renderPlan(WorkoutPlan plan) {
        // Fill header card
        tvPlanTitle.setText(plan.planTitle);
        tvPlanDescription.setText(plan.planDescription);
        tvAdaptationNote.setText(plan.adaptationNote);
        tvPlanTarget.setText(plan.weeklyTarget + " days");
        tvPlanScoreBasis.setText(Math.round(plan.currentScore) + " / 100");

        // Build day list
        planDayContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (WorkoutPlanDay day : plan.days) {
            View item = inflater.inflate(R.layout.item_plan_day,
                    planDayContainer, false);

            TextView tvIcon      = item.findViewById(R.id.tvPlanDayIcon);
            TextView tvLabel     = item.findViewById(R.id.tvPlanDayLabel);
            TextView tvFocus     = item.findViewById(R.id.tvPlanDayFocus);
            TextView tvReasoning = item.findViewById(R.id.tvPlanDayReasoning);
            TextView tvIntensity = item.findViewById(R.id.tvPlanDayIntensity);
            TextView tvDuration  = item.findViewById(R.id.tvPlanDayDuration);

            tvIcon.setText(workoutEmoji(day.workoutType));
            tvLabel.setText(day.dayLabel);
            tvFocus.setText(day.isRestDay ? "Rest Day" : day.focus);
            tvReasoning.setText(day.reasoning);
            tvIntensity.setText(day.intensity);
            tvDuration.setText(day.isRestDay ? "—"
                    : day.durationMinutes + " min");

            // Dim rest days
            if (day.isRestDay) {
                item.setAlpha(0.6f);
                tvIcon.setBackgroundColor(0xFFF5F5F5);
            }

            // Highlight today
            if (day.dayNumber == 1) {
                item.setBackgroundColor(0xFFF1F8F2);
            }

            planDayContainer.addView(item);
        }
    }

    private String workoutEmoji(String type) {
        switch (type) {
            case "strength":    return "💪";
            case "cardio":      return "🏃";
            case "flexibility": return "🧘";
            case "rest":        return "😴";
            default:            return "🏋️";
        }
    }
}