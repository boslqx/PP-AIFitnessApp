package com.example.aifitnessapp.ui.progress;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.viewmodel.ProgressViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {

    private ProgressViewModel viewModel;

    private View loadingState, contentState;
    private TextView tvTotalWorkouts, tvCompletionPct, tvStreak;
    private TextView tvTotalSkipped, tvMemberSince;
    private LinearLayout historyContainer, breakdownContainer;
    private MaterialButton btnBackFromProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        bindViews();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            viewModel.loadProgress(user.id);
        });

        // Loading state
        viewModel.isLoading.observe(this, loading -> {
            loadingState.setVisibility(loading ? View.VISIBLE : View.GONE);
            contentState.setVisibility(loading ? View.GONE : View.VISIBLE);
        });

        // Stats
        viewModel.totalWorkouts.observe(this, v ->
                tvTotalWorkouts.setText(String.valueOf(v)));

        viewModel.completionPct.observe(this, v ->
                tvCompletionPct.setText(v + "%"));

        viewModel.currentStreak.observe(this, v ->
                tvStreak.setText(v + " 🔥"));

        viewModel.totalSkipped.observe(this, v ->
                tvTotalSkipped.setText(v + " skipped"));

        viewModel.memberSince.observe(this, v ->
                tvMemberSince.setText("Member since " + v));

        // History
        viewModel.historyRows.observe(this, rows -> {
            if (rows == null) return;
            buildHistoryList(rows);
        });

        // Activity breakdown
        viewModel.activityBreakdown.observe(this, map -> {
            if (map == null) return;
            buildBreakdown(map);
        });

        btnBackFromProgress.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    // ── History list ──────────────────────────────────────────

    private void buildHistoryList(List<ProgressViewModel.HistoryRow> rows) {
        historyContainer.removeAllViews();

        if (rows.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No workouts logged yet.\nComplete your first session to see history.");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(14f);
            empty.setPadding(0, 16, 0, 0);
            historyContainer.addView(empty);
            return;
        }

        // Show most recent first — rows are ASC from DB so reverse
        for (int i = rows.size() - 1; i >= 0; i--) {
            ProgressViewModel.HistoryRow row = rows.get(i);
            View item = LayoutInflater.from(this)
                    .inflate(R.layout.item_history_row, historyContainer, false);

            TextView tvIcon     = item.findViewById(R.id.tvHistoryIcon);
            TextView tvDate     = item.findViewById(R.id.tvHistoryDate);
            TextView tvTitle    = item.findViewById(R.id.tvHistoryTitle);
            TextView tvEffort   = item.findViewById(R.id.tvHistoryEffort);

            tvIcon.setText(row.isRestDay ? "😴" : activityEmoji(row.activityType));
            tvDate.setText(row.date);
            tvTitle.setText(row.statusIcon + "  " + row.sessionTitle);

            if (!row.isRestDay && row.perceivedEffort > 0) {
                tvEffort.setText(effortDots(row.perceivedEffort));
                tvEffort.setVisibility(View.VISIBLE);
            } else {
                tvEffort.setVisibility(View.GONE);
            }

            // Dim skipped rows
            if ("SKIPPED".equals(row.completionStatus)) {
                item.setAlpha(0.5f);
            }

            historyContainer.addView(item);
        }
    }

    // ── Activity breakdown ────────────────────────────────────

    private void buildBreakdown(Map<String, Integer> map) {
        breakdownContainer.removeAllViews();

        if (map.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No activity data yet.");
            empty.setTextColor(0xFF9E9E9E);
            breakdownContainer.addView(empty);
            return;
        }

        // Find max count for bar scaling
        int maxCount = 1;
        for (int count : map.values()) {
            if (count > maxCount) maxCount = count;
        }

        // Sort by count descending
        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());

        for (Map.Entry<String, Integer> entry : entries) {
            View item = LayoutInflater.from(this)
                    .inflate(R.layout.item_breakdown_row, breakdownContainer, false);

            TextView tvEmoji = item.findViewById(R.id.tvBreakdownEmoji);
            TextView tvName  = item.findViewById(R.id.tvBreakdownName);
            TextView tvBar   = item.findViewById(R.id.tvBreakdownBar);
            TextView tvCount = item.findViewById(R.id.tvBreakdownCount);

            tvEmoji.setText(activityEmoji(entry.getKey()));
            tvName.setText(formatActivity(entry.getKey()));
            tvCount.setText(String.valueOf(entry.getValue()));

            // Scale bar to max — 12 chars wide
            float ratio = (float) entry.getValue() / maxCount;
            int filled = Math.max(1, Math.round(ratio * 12));
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                bar.append(i < filled ? "█" : "░");
            }
            tvBar.setText(bar.toString());

            breakdownContainer.addView(item);
        }
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        loadingState        = findViewById(R.id.loadingState);
        contentState        = findViewById(R.id.contentState);
        tvTotalWorkouts     = findViewById(R.id.tvTotalWorkouts);
        tvCompletionPct     = findViewById(R.id.tvCompletionPct);
        tvStreak            = findViewById(R.id.tvBestStreak);
        tvTotalSkipped      = findViewById(R.id.tvTotalSkipped);
        tvMemberSince       = findViewById(R.id.tvMemberSince);
        historyContainer    = findViewById(R.id.historyContainer);
        breakdownContainer  = findViewById(R.id.breakdownContainer);
        btnBackFromProgress = findViewById(R.id.btnBackFromProgress);
    }

    // ── Helpers ───────────────────────────────────────────────

    private String activityEmoji(String type) {
        if (type == null) return "💪";
        switch (type) {
            case "GYM":        return "🏋️";
            case "RUNNING":    return "🏃";
            case "BOULDERING": return "🧗";
            case "CYCLING":    return "🚴";
            case "SWIMMING":   return "🏊";
            case "YOGA":       return "🧘";
            case "HOME":       return "🏠";
            case "SPORTS":     return "⚽";
            case "REST":       return "😴";
            default:           return "💪";
        }
    }

    private String formatActivity(String type) {
        if (type == null) return "Workout";
        switch (type) {
            case "GYM":        return "Gym";
            case "RUNNING":    return "Running";
            case "BOULDERING": return "Bouldering";
            case "CYCLING":    return "Cycling";
            case "SWIMMING":   return "Swimming";
            case "YOGA":       return "Yoga";
            case "HOME":       return "Home";
            case "SPORTS":     return "Sports";
            case "REST":       return "Rest";
            default:           return type;
        }
    }

    private String effortDots(int effort) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= effort ? "●" : "○");
        }
        return sb.toString();
    }
}