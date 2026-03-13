package com.example.aifitnessapp.ui.consistency;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.ConsistencyScore;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.ConsistencyViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConsistencyActivity extends AppCompatActivity {

    private ConsistencyViewModel viewModel;

    private TextView tvWeeklyScore, tvStreakCount, tvWeekTrend;
    private TextView tvBurnoutRiskBig, tvCoachMessage;
    private LinearLayout barChartContainer, dayLabelsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consistency);

        viewModel = new ViewModelProvider(this).get(ConsistencyViewModel.class);

        bindViews();

        // Load userId first, then scores
        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            viewModel.loadScores(user.id);

            // Observe the 7-day scores LiveData
            viewModel.last7Scores.observe(this, scores -> {
                viewModel.analyzeScores(scores);
                buildBarChart(scores);
            });
        });

        observeAnalysis();

        findViewById(R.id.btnBackFromConsistency).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void bindViews() {
        tvWeeklyScore      = findViewById(R.id.tvWeeklyScore);
        tvStreakCount      = findViewById(R.id.tvStreakCount);
        tvWeekTrend        = findViewById(R.id.tvWeekTrend);
        tvBurnoutRiskBig   = findViewById(R.id.tvBurnoutRiskBig);
        tvCoachMessage     = findViewById(R.id.tvCoachMessage);
        barChartContainer  = findViewById(R.id.barChartContainer);
        dayLabelsContainer = findViewById(R.id.dayLabelsContainer);
    }

    private void observeAnalysis() {
        viewModel.weeklyAvgScore.observe(this, score -> {
            tvWeeklyScore.setText(String.valueOf(Math.round(score)));
        });

        viewModel.currentStreak.observe(this, streak -> {
            tvStreakCount.setText(streak + " days 🔥");
        });

        viewModel.weekTrend.observe(this, trend -> {
            tvWeekTrend.setText(trend);
        });

        viewModel.burnoutRisk.observe(this, risk -> {
            tvBurnoutRiskBig.setText(risk);
            switch (risk) {
                case "HIGH":   tvBurnoutRiskBig.setTextColor(Color.parseColor("#FF5252")); break;
                case "MEDIUM": tvBurnoutRiskBig.setTextColor(Color.parseColor("#FFD740")); break;
                default:       tvBurnoutRiskBig.setTextColor(Color.WHITE);
            }
        });

        viewModel.coachMessage.observe(this, msg -> {
            tvCoachMessage.setText(msg);
        });
    }

    /*
     * Builds a simple bar chart programmatically — no external library needed.
     * Each bar is a View with height proportional to the score (max 160dp).
     */
    private void buildBarChart(List<ConsistencyScore> scores) {
        barChartContainer.removeAllViews();
        dayLabelsContainer.removeAllViews();

        if (scores == null || scores.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Log daily activity to see your chart");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setTextSize(13f);
            barChartContainer.addView(empty);
            return;
        }

        // Scores come in DESC order (newest first) — reverse for chart (oldest left)
        int count = scores.size();
        float maxHeight = 140f; // dp — max bar height
        float density   = getResources().getDisplayMetrics().density;

        for (int i = count - 1; i >= 0; i--) {
            ConsistencyScore cs = scores.get(i);

            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            colParams.setMargins(4, 0, 4, 0);
            column.setLayoutParams(colParams);

            // Score label above bar
            TextView scoreLabel = new TextView(this);
            scoreLabel.setText(String.valueOf(Math.round(cs.score)));
            scoreLabel.setTextSize(9f);
            scoreLabel.setTextColor(Color.parseColor("#757575"));
            scoreLabel.setGravity(Gravity.CENTER);
            scoreLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // The bar itself
            View bar = new View(this);
            int barHeightDp = Math.max(Math.round((cs.score / 100f) * maxHeight), 4);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (barHeightDp * density));
            bar.setLayoutParams(barParams);
            bar.setBackgroundColor(barColor(cs.score));

            column.addView(scoreLabel);
            column.addView(bar);
            barChartContainer.addView(column);

            // Day label below chart
            TextView dayLabel = new TextView(this);
            dayLabel.setText(getDayLabel(cs.date));
            dayLabel.setTextSize(10f);
            dayLabel.setTextColor(Color.parseColor("#9E9E9E"));
            dayLabel.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            labelParams.setMargins(4, 4, 4, 0);
            dayLabel.setLayoutParams(labelParams);
            dayLabelsContainer.addView(dayLabel);
        }
    }

    // Color the bar: green = good, amber = okay, red = low
    private int barColor(float score) {
        if (score >= 70) return Color.parseColor("#4CAF50"); // green
        if (score >= 45) return Color.parseColor("#FFA726"); // amber
        return Color.parseColor("#EF5350");                  // red
    }

    // "2025-03-11" → "Tue"
    private String getDayLabel(String dateStr) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return "—";
        }
    }
}