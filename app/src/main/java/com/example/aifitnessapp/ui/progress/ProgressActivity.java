package com.example.aifitnessapp.ui.progress;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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

    private View           loadingState, contentState;
    private TextView       tvTotalWorkouts, tvCompletionPct, tvStreak;
    private TextView       tvTotalSkipped, tvMemberSince;
    private LinearLayout   historyContainer, breakdownContainer;
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

        viewModel.isLoading.observe(this, loading -> {
            loadingState.setVisibility(loading ? View.VISIBLE : View.GONE);
            contentState.setVisibility(loading ? View.GONE  : View.VISIBLE);
        });

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

        viewModel.historyRows.observe(this, rows -> {
            if (rows == null) return;
            buildHistoryList(rows);
        });

        viewModel.activityBreakdown.observe(this, map -> {
            if (map == null) return;
            buildBreakdown(map);
        });

        btnBackFromProgress.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    // ─────────────────────────────────────────────────────────
    //  HISTORY LIST
    //
    //  Each row shows: [photo thumbnail OR emoji icon] | date + title | effort
    //
    //  PHOTO LOADING LOGIC:
    //  1. Check if row.photoPath is non-null and non-empty
    //  2. If yes: hide the emoji TextView, show the ImageView, load the URI
    //  3. If no:  show the emoji TextView, keep ImageView gone
    //
    //  WHY check both null AND empty?
    //  The database stores an empty string "" if the user didn't add a photo
    //  (some code paths call saveLog with photoPath = null, others with "").
    //  Checking both makes the code robust against either case.
    // ─────────────────────────────────────────────────────────
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

            // ── Bind views ────────────────────────────────────
            TextView  tvIcon    = item.findViewById(R.id.tvHistoryIcon);
            ImageView ivPhoto   = item.findViewById(R.id.ivHistoryPhoto); // ← NEW
            TextView  tvDate    = item.findViewById(R.id.tvHistoryDate);
            TextView  tvTitle   = item.findViewById(R.id.tvHistoryTitle);
            TextView  tvEffort  = item.findViewById(R.id.tvHistoryEffort);

            // ── Photo vs icon logic ───────────────────────────
            boolean hasPhoto = row.photoPath != null && !row.photoPath.isEmpty();

            if (hasPhoto) {
                // Hide the emoji text icon — the photo replaces it
                tvIcon.setVisibility(View.GONE);

                // Show the ImageView and load the photo
                ivPhoto.setVisibility(View.VISIBLE);
                try {
                    // Uri.parse() converts our stored path string into a Uri.
                    // The path was saved as "file:///..." by LogActivity,
                    // or as a content:// URI from the gallery picker.
                    // Uri.parse() handles both formats correctly.
                    ivPhoto.setImageURI(Uri.parse(row.photoPath));
                } catch (Exception e) {
                    // If the file was deleted or the path is corrupt,
                    // fall back gracefully to the emoji icon.
                    ivPhoto.setVisibility(View.GONE);
                    tvIcon.setVisibility(View.VISIBLE);
                    tvIcon.setText(row.isRestDay ? "😴" : activityEmoji(row.activityType));
                }
            } else {
                // No photo — show emoji icon as before
                ivPhoto.setVisibility(View.GONE);
                tvIcon.setVisibility(View.VISIBLE);
                tvIcon.setText(row.isRestDay ? "😴" : activityEmoji(row.activityType));
            }

            // ── Date and title ────────────────────────────────
            tvDate.setText(row.date);
            tvTitle.setText(row.statusIcon + "  " + row.sessionTitle);

            // ── Effort dots ───────────────────────────────────
            if (!row.isRestDay && row.perceivedEffort > 0) {
                tvEffort.setText(effortDots(row.perceivedEffort));
                tvEffort.setVisibility(View.VISIBLE);
            } else {
                tvEffort.setVisibility(View.GONE);
            }

            // Dim skipped rows so they read as less significant
            if ("SKIPPED".equals(row.completionStatus)) {
                item.setAlpha(0.5f);
            }

            historyContainer.addView(item);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIVITY BREAKDOWN — unchanged from original
    // ─────────────────────────────────────────────────────────
    private void buildBreakdown(Map<String, Integer> map) {
        breakdownContainer.removeAllViews();

        if (map.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No activity data yet.");
            empty.setTextColor(0xFF9E9E9E);
            breakdownContainer.addView(empty);
            return;
        }

        int maxCount = 1;
        for (int count : map.values()) {
            if (count > maxCount) maxCount = count;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
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

            float ratio = (float) entry.getValue() / maxCount;
            int filled  = Math.max(1, Math.round(ratio * 12));
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 12; i++) bar.append(i < filled ? "█" : "░");
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
        for (int i = 1; i <= 5; i++) sb.append(i <= effort ? "●" : "○");
        return sb.toString();
    }
}