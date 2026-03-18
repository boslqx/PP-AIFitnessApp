package com.example.aifitnessapp.ui.workout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.WorkoutHistoryViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private WorkoutHistoryViewModel viewModel;
    private LinearLayout sessionListContainer;

    // Header stat views
    private TextView tvTotalWorkouts, tvTotalTime, tvTotalCalories;
    private TextView tvFavouriteType, tvAvgIntensity, tvBestStreak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        viewModel = new ViewModelProvider(this).get(WorkoutHistoryViewModel.class);

        bindViews();
        observeStats();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            viewModel.loadSessions(user.id);
            viewModel.computeStats(user.id);

            // Observe the live session list
            viewModel.recentSessions.observe(this, sessions -> {
                buildSessionList(sessions);
            });
        });

        findViewById(R.id.btnBackFromHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void bindViews() {
        sessionListContainer = findViewById(R.id.sessionListContainer);
        tvTotalWorkouts      = findViewById(R.id.tvTotalWorkouts);
        tvTotalTime          = findViewById(R.id.tvTotalTime);
        tvTotalCalories      = findViewById(R.id.tvTotalCalories);
        tvFavouriteType      = findViewById(R.id.tvFavouriteType);
        tvAvgIntensity       = findViewById(R.id.tvAvgIntensity);
        tvBestStreak         = findViewById(R.id.tvBestStreak);
    }

    private void observeStats() {
        viewModel.totalWorkouts.observe(this, v ->
                tvTotalWorkouts.setText(String.valueOf(v)));

        viewModel.totalMinutes.observe(this, mins -> {
            int h = mins / 60, m = mins % 60;
            tvTotalTime.setText(h + "h " + m + "m");
        });

        viewModel.totalCalories.observe(this, v ->
                tvTotalCalories.setText(String.valueOf(v)));

        viewModel.favoriteType.observe(this, v ->
                tvFavouriteType.setText(v));

        viewModel.avgIntensity.observe(this, v ->
                tvAvgIntensity.setText(String.format(Locale.getDefault(), "%.1f / 5", v)));

        viewModel.bestStreak.observe(this, v ->
                tvBestStreak.setText(v));
    }

    /*
     * Builds the session list programmatically by inflating item_workout_session.xml
     * for each WorkoutSession and populating it with data.
     *
     * WHY PROGRAMMATIC instead of RecyclerView:
     * Simpler to learn at this stage. RecyclerView is better for very long lists
     * but adds adapter/ViewHolder complexity we don't need yet.
     * We cap at 30 sessions so performance is fine with a ScrollView.
     */
    private void buildSessionList(List<WorkoutSession> sessions) {
        sessionListContainer.removeAllViews();

        if (sessions == null || sessions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No workouts logged yet.\nHead to Log Today to add your first session!");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(14f);
            empty.setPadding(0, 48, 0, 0);
            sessionListContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (WorkoutSession session : sessions) {
            View item = inflater.inflate(R.layout.item_workout_session,
                    sessionListContainer, false);

            // Populate each field
            TextView tvIcon      = item.findViewById(R.id.tvWorkoutIcon);
            TextView tvType      = item.findViewById(R.id.tvWorkoutType);
            TextView tvDate      = item.findViewById(R.id.tvWorkoutDate);
            TextView tvDuration  = item.findViewById(R.id.tvWorkoutDuration);
            TextView tvCalories  = item.findViewById(R.id.tvWorkoutCalories);
            TextView tvDots      = item.findViewById(R.id.tvIntensityDots);

            tvIcon.setText(workoutEmoji(session.workoutType));
            tvType.setText(formatType(session.workoutType));
            tvDate.setText(formatDate(session.date));
            tvDuration.setText(session.durationMinutes + " min");
            tvCalories.setText(session.caloriesBurned + " kcal");
            tvDots.setText(intensityDots(session.intensityLevel));

            // Dim rest days slightly
            if (session.workoutType.equals("rest")) {
                item.setAlpha(0.6f);
            }

            sessionListContainer.addView(item);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private String workoutEmoji(String type) {
        switch (type) {
            case "strength":    return "💪";
            case "cardio":      return "🏃";
            case "flexibility": return "🧘";
            case "rest":        return "😴";
            default:            return "🏋️";
        }
    }

    private String formatType(String type) {
        switch (type) {
            case "strength":    return "Strength Training";
            case "cardio":      return "Cardio";
            case "flexibility": return "Flexibility";
            case "rest":        return "Rest Day";
            default:            return type;
        }
    }

    // "2025-03-11" → "Tue, Mar 11"
    private String formatDate(String dateStr) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            return new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return dateStr;
        }
    }

    /*
     * Renders intensity as filled/empty dots.
     * intensity=3 → "●●●○○"
     * Makes it easy to scan effort level at a glance.
     */
    private String intensityDots(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= level ? "●" : "○");
        }
        return sb.toString();
    }
}