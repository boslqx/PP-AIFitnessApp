package com.example.aifitnessapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.ui.log.LogActivity;
import com.example.aifitnessapp.ui.plan.PlanActivity;
import com.example.aifitnessapp.viewmodel.HomeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import android.widget.LinearLayout;

public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;

    // Header
    private TextView tvGreeting, tvWeekLabel;

    // Today card
    private MaterialCardView cardToday;
    private TextView tvTodayEmoji, tvTodayTitle, tvTodayDetail;
    private TextView tvTodayIntensity, tvTodayDuration, tvTodayCoachNote;
    private MaterialButton btnStartWorkout, btnRestDay;
    private TextView tvAlreadyLogged;

    // Week strip
    private LinearLayout weekStrip;

    // Bottom nav
    private MaterialButton btnNavLog, btnNavPlan, btnNavProgress, btnNavCoach;

    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        bindViews();
        setupBottomNav();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            currentUserId = user.id;

            // 1. Greeting
            tvGreeting.setText(viewModel.getGreeting(user.name));

            // 2. Init plan (generates if missing)
            viewModel.initPlan(user.id);

            // 3. Week label
            viewModel.loadWeekLabel(user.id);
        });

        // Week label / coach message
        viewModel.coachMessage.observe(this, msg ->
                tvWeekLabel.setText(msg));

        // Once plan is confirmed ready, load LiveData
        viewModel.planReady.observe(this, ready -> {
            if (!ready || currentUserId == -1) return;
            viewModel.loadPlan(currentUserId);
            observePlan();
        });
    }

    private void observePlan() {

        // TODAY card
        viewModel.todayPlan.observe(this, plan -> {
            if (plan == null) return;
            renderTodayCard(plan);
        });

        // WEEK strip
        viewModel.weekPlan.observe(this, plans -> {
            if (plans == null || plans.isEmpty()) return;
            renderWeekStrip(plans);
        });
    }

    // ── Today card ────────────────────────────────────────────

    private void renderTodayCard(PlannedWorkout plan) {
        tvTodayEmoji.setText(activityEmoji(plan.activityType));
        tvTodayTitle.setText(plan.sessionTitle);
        tvTodayDetail.setText(plan.sessionDetail);
        tvTodayCoachNote.setText(plan.coachNote);

        if (plan.isRestDay) {
            tvTodayIntensity.setText("Rest");
            tvTodayDuration.setText("—");
            btnStartWorkout.setVisibility(View.GONE);
            btnRestDay.setVisibility(View.VISIBLE);
        } else {
            tvTodayIntensity.setText(intensityLabel(plan.intensityLevel)
                    + "  ·  " + plan.durationMinutes + " min");
            tvTodayDuration.setVisibility(View.GONE);
            btnStartWorkout.setVisibility(View.VISIBLE);
            btnRestDay.setVisibility(View.GONE);
        }

        // Check if already logged today
        if (currentUserId != -1) {
            viewModel.getTodayLog(currentUserId).observe(this, log -> {
                if (log != null) {
                    tvAlreadyLogged.setVisibility(View.VISIBLE);
                    btnStartWorkout.setText("Log Again");
                } else {
                    tvAlreadyLogged.setVisibility(View.GONE);
                    btnStartWorkout.setText("Start Workout →");
                }
            });
        }

        // Start workout → go to log screen with plan context
        btnStartWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogActivity.class);
            intent.putExtra("plannedWorkoutId", plan.id);
            intent.putExtra("activityType", plan.activityType);
            intent.putExtra("sessionTitle", plan.sessionTitle);
            startActivity(intent);
        });

        btnRestDay.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogActivity.class);
            intent.putExtra("plannedWorkoutId", plan.id);
            intent.putExtra("activityType", "REST");
            intent.putExtra("sessionTitle", "Rest Day");
            startActivity(intent);
        });
    }

    // ── Week strip ────────────────────────────────────────────

    private void renderWeekStrip(List<PlannedWorkout> plans) {
        weekStrip.removeAllViews();
        String today = PlanEngine.getTodayDayOfWeek();

        for (PlannedWorkout plan : plans) {
            // Inflate without attaching — we set params manually
            View dayView = getLayoutInflater()
                    .inflate(R.layout.item_week_day, weekStrip, false);

            // Set weight programmatically so each day takes equal space
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dayView.setLayoutParams(params);

            TextView tvDayLabel = dayView.findViewById(R.id.tvDayLabel);
            TextView tvDayIcon  = dayView.findViewById(R.id.tvDayIcon);
            View     indicator  = dayView.findViewById(R.id.todayIndicator);

            // Safety check — skip if layout IDs not found
            if (tvDayLabel == null || tvDayIcon == null || indicator == null) continue;

            tvDayLabel.setText(plan.dayOfWeek.substring(0, 1)
                    + plan.dayOfWeek.substring(1, 2).toLowerCase());
            tvDayIcon.setText(plan.isRestDay ? "😴" : activityEmoji(plan.activityType));

            boolean isToday = plan.dayOfWeek.equals(today);
            indicator.setVisibility(isToday ? View.VISIBLE : View.INVISIBLE);

            if (isToday) {
                tvDayLabel.setTextColor(getColor(R.color.md_theme_primary));
                tvDayIcon.setAlpha(1f);
            } else {
                tvDayLabel.setTextColor(0xFF9E9E9E);
                tvDayIcon.setAlpha(isDayPast(plan.dayOfWeek, today) ? 0.4f : 1f);
            }

            weekStrip.addView(dayView);
        }
    }

    // ── Bottom nav ────────────────────────────────────────────

    private void setupBottomNav() {
        btnNavLog.setOnClickListener(v ->
                startActivity(new Intent(this, LogActivity.class)));

        btnNavPlan.setOnClickListener(v ->
                startActivity(new Intent(this, PlanActivity.class)));

        // Progress and Coach — stubs for now
        btnNavProgress.setOnClickListener(v -> {
            // TODO: Phase B — ProgressActivity
        });

        btnNavCoach.setOnClickListener(v -> {
            // TODO: Phase B — CoachActivity
        });
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        tvGreeting        = findViewById(R.id.tvGreeting);
        tvWeekLabel       = findViewById(R.id.tvWeekLabel);
        cardToday         = findViewById(R.id.cardToday);
        tvTodayEmoji      = findViewById(R.id.tvTodayEmoji);
        tvTodayTitle      = findViewById(R.id.tvTodayTitle);
        tvTodayDetail     = findViewById(R.id.tvTodayDetail);
        tvTodayIntensity  = findViewById(R.id.tvTodayIntensity);
        tvTodayDuration   = findViewById(R.id.tvTodayDuration);
        tvTodayCoachNote  = findViewById(R.id.tvTodayCoachNote);
        btnStartWorkout   = findViewById(R.id.btnStartWorkout);
        btnRestDay        = findViewById(R.id.btnRestDay);
        tvAlreadyLogged   = findViewById(R.id.tvAlreadyLogged);
        weekStrip         = findViewById(R.id.weekStrip);
        btnNavLog         = findViewById(R.id.btnNavLog);
        btnNavPlan        = findViewById(R.id.btnNavPlan);
        btnNavProgress    = findViewById(R.id.btnNavProgress);
        btnNavCoach       = findViewById(R.id.btnNavCoach);
    }

    // ── Helpers ───────────────────────────────────────────────

    private String activityEmoji(String type) {
        if (type == null) return "🏋️";
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

    private String intensityLabel(int level) {
        switch (level) {
            case 1: return "Very Light";
            case 2: return "Light";
            case 3: return "Moderate";
            case 4: return "Hard";
            case 5: return "Very Hard";
            default: return "Moderate";
        }
    }

    private boolean isDayPast(String day, String today) {
        String[] order = {"MON","TUE","WED","THU","FRI","SAT","SUN"};
        int dayIdx = 0, todayIdx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(day))   dayIdx   = i;
            if (order[i].equals(today)) todayIdx = i;
        }
        return dayIdx < todayIdx;
    }
}