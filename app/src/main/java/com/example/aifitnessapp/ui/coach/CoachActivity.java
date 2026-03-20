package com.example.aifitnessapp.ui.coach;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.engine.CoachEngine;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.viewmodel.CoachViewModel;
import com.google.android.material.button.MaterialButton;

public class CoachActivity extends AppCompatActivity {

    private CoachViewModel viewModel;

    private View loadingState, contentState;
    private TextView tvCoachMessage, tvAdaptationNotice, tvAdaptationStrategy;
    private TextView tvCompletionFraction, tvCompletionRate, tvCompletionBar;
    private TextView tvStreak, tvAvgEffort, tvEffortBar;
    private TextView tvNextIntensity, tvWeekLabel;
    private MaterialButton btnBackFromCoach;

    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach);

        viewModel = new ViewModelProvider(this).get(CoachViewModel.class);

        bindViews();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            currentUserId = user.id;
            viewModel.loadReport(user.id);

            // Week label
            String weekStart = PlanEngine.getCurrentWeekStart();
            String today = PlanEngine.getTodayDayOfWeek();
            tvWeekLabel.setText("Week summary  ·  " + today.charAt(0)
                    + today.substring(1).toLowerCase());
        });

        // Loading state
        viewModel.isLoading.observe(this, loading -> {
            loadingState.setVisibility(loading ? View.VISIBLE : View.GONE);
            contentState.setVisibility(loading ? View.GONE : View.VISIBLE);
        });

        // Render report
        viewModel.report.observe(this, report -> {
            if (report == null) return;
            renderReport(report);
        });

        btnBackFromCoach.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void renderReport(CoachEngine.CoachReport r) {

        // ── Coach message ────────────────────────────────────
        tvCoachMessage.setText(r.coachMessage);

        // ── This week completion ─────────────────────────────
        tvCompletionFraction.setText(r.completedWorkouts + " / " + r.plannedWorkouts
                + " workouts done");

        int pct = r.plannedWorkouts > 0
                ? Math.round(r.completionRate * 100) : 0;
        tvCompletionRate.setText(pct + "%");
        tvCompletionBar.setText(buildBar(r.completionRate, 10));

        // ── Streak ───────────────────────────────────────────
        if (r.streakDays > 0) {
            tvStreak.setText("🔥 " + r.streakDays
                    + (r.streakDays == 1 ? " session" : " sessions") + " this week");
        } else {
            tvStreak.setText("No sessions logged yet this week");
        }

        // ── Effort ───────────────────────────────────────────
        if (r.avgEffort > 0) {
            tvAvgEffort.setText(String.format("%.1f / 5.0  —  %s",
                    r.avgEffort, effortLabel(r.avgEffort)));
            tvEffortBar.setText(buildBar(r.avgEffort / 5f, 10));
        } else {
            tvAvgEffort.setText("No effort data yet");
            tvEffortBar.setText("──────────");
        }

        // ── Adaptation notice ────────────────────────────────
        tvAdaptationNotice.setText(r.adaptationNotice);

        // Strategy badge
        tvAdaptationStrategy.setText(strategyLabel(r.adaptationStrategy));
        tvAdaptationStrategy.setTextColor(strategyColor(r.adaptationStrategy));

        // Next week intensity
        tvNextIntensity.setText("Next week intensity: " + r.nextIntensityLabel);
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        loadingState         = findViewById(R.id.loadingState);
        contentState         = findViewById(R.id.contentState);
        tvCoachMessage       = findViewById(R.id.tvCoachMessage);
        tvAdaptationNotice   = findViewById(R.id.tvAdaptationNotice);
        tvAdaptationStrategy = findViewById(R.id.tvAdaptationStrategy);
        tvCompletionFraction = findViewById(R.id.tvCompletionFraction);
        tvCompletionRate     = findViewById(R.id.tvCompletionRate);
        tvCompletionBar      = findViewById(R.id.tvCompletionBar);
        tvStreak             = findViewById(R.id.tvStreak);
        tvAvgEffort          = findViewById(R.id.tvAvgEffort);
        tvEffortBar          = findViewById(R.id.tvEffortBar);
        tvNextIntensity      = findViewById(R.id.tvNextIntensity);
        tvWeekLabel          = findViewById(R.id.tvCoachWeekLabel);
        btnBackFromCoach     = findViewById(R.id.btnBackFromCoach);
    }

    // ── Helpers ───────────────────────────────────────────────

    /*
     * Builds a text progress bar e.g. "████████░░" for 80%
     * Uses filled block ● and empty ○ characters
     */
    private String buildBar(float ratio, int length) {
        int filled = Math.round(ratio * length);
        filled = Math.max(0, Math.min(filled, length));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        return sb.toString();
    }

    private String effortLabel(float effort) {
        if (effort <= 1.5f) return "Very Easy";
        if (effort <= 2.5f) return "Light";
        if (effort <= 3.5f) return "Moderate";
        if (effort <= 4.5f) return "Hard";
        return "Very Hard";
    }

    private String strategyLabel(String strategy) {
        switch (strategy) {
            case "RECOVERY":    return "⚠️ Recovery Week";
            case "PROGRESSIVE": return "🔥 Progressive";
            case "CATCH_UP":    return "💪 Catch Up";
            default:            return "✅ On Track";
        }
    }

    private int strategyColor(String strategy) {
        switch (strategy) {
            case "RECOVERY":    return 0xFFD32F2F;
            case "PROGRESSIVE": return 0xFF2E7D32;
            case "CATCH_UP":    return 0xFFE65100;
            default:            return 0xFF1565C0;
        }
    }
}