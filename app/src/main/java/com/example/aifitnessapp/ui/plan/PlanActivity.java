// ui/plan/PlanActivity.java
package com.example.aifitnessapp.ui.plan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.ui.log.LogActivity;
import com.example.aifitnessapp.viewmodel.PlanViewModel;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Map;

public class PlanActivity extends AppCompatActivity {

    private PlanViewModel viewModel;

    private TextView tvPlanSummary, tvPlanWeekDates;
    private LinearLayout planDayContainer;
    private MaterialButton btnBackFromPlan;

    private int currentUserId = -1;
    public LiveData<List<PlannedWorkout>> weekPlan =
            new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        viewModel = new ViewModelProvider(this).get(PlanViewModel.class);

        bindViews();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            currentUserId = user.id;
            viewModel.loadPlan(user.id);
        });

        viewModel.planReady.observe(this, ready -> {
            if (!ready || currentUserId == -1) return;

            // Init week plan LiveData FIRST
            viewModel.initWeekPlan(currentUserId);
            viewModel.loadLogMap(currentUserId);
            loadWeekDates(currentUserId);

            // NOW it's safe to observe weekPlan — it's been assigned
            viewModel.weekPlan.observe(this, plans -> {
                if (plans == null || plans.isEmpty()) return;

                new Thread(() -> {
                    int weekNum = viewModel.getCurrentWeekNum(currentUserId);
                    runOnUiThread(() -> {
                        viewModel.buildSummary(plans, weekNum);
                        renderPlan(plans);
                    });
                }).start();
            });

            // Observe log map — re-render when logs update
            viewModel.logMap.observe(this, map -> {
                if (viewModel.weekPlan.getValue() != null
                        && !viewModel.weekPlan.getValue().isEmpty()) {
                    renderPlan(viewModel.weekPlan.getValue());
                }
            });
        });

        // Summary label
        viewModel.planSummary.observe(this, summary ->
                tvPlanSummary.setText(summary));

        // Back button
        btnBackFromPlan.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    // Helper to call loadWeekLabel on background thread
    private void loadWeekDates(int userId) {
        com.example.aifitnessapp.util.AppExecutors.getInstance().diskIO().execute(() -> {
            int weekNum = viewModel.getCurrentWeekNum(userId);
            String weekStart = PlanEngine.getCurrentWeekStart();
            // Format: "19 Mar – 25 Mar"
            String dates = formatWeekDates(weekStart);
            runOnUiThread(() -> tvPlanWeekDates.setText(dates));
        });
    }

    // ── Render ────────────────────────────────────────────────

    private void renderPlan(List<PlannedWorkout> plans) {
        planDayContainer.removeAllViews();
        String today = PlanEngine.getTodayDayOfWeek();
        Map<Integer, WorkoutLog> logs = viewModel.logMap.getValue();

        for (PlannedWorkout plan : plans) {
            View item = LayoutInflater.from(this)
                    .inflate(R.layout.item_plan_day, planDayContainer, false);

            TextView tvDayName    = item.findViewById(R.id.tvPlanDayName);
            TextView tvDayEmoji   = item.findViewById(R.id.tvPlanDayEmoji);
            TextView tvDayTitle   = item.findViewById(R.id.tvPlanDayTitle);
            TextView tvDayDetail  = item.findViewById(R.id.tvPlanDayDetail);
            TextView tvDayMeta    = item.findViewById(R.id.tvPlanDayMeta);
            TextView tvCoachNote  = item.findViewById(R.id.tvPlanDayCoachNote);
            TextView tvLogStatus  = item.findViewById(R.id.tvPlanDayLogStatus);
            MaterialButton btnLog = item.findViewById(R.id.btnPlanDayLog);
            View todayBar         = item.findViewById(R.id.todayBar);

            // Day name
            tvDayName.setText(fullDayName(plan.dayOfWeek));

            boolean isToday = plan.dayOfWeek.equals(today);
            boolean isPast  = isDayPast(plan.dayOfWeek, today);

            // Today highlight
            todayBar.setVisibility(isToday ? View.VISIBLE : View.GONE);

            if (plan.isRestDay) {
                tvDayEmoji.setText("😴");
                tvDayTitle.setText("Rest Day");
                tvDayDetail.setText(plan.sessionDetail);
                tvDayMeta.setText("Recovery");
                tvCoachNote.setText(plan.coachNote);
                tvLogStatus.setVisibility(View.GONE);
                btnLog.setVisibility(View.GONE);
                item.setAlpha(0.6f);
            } else {
                tvDayEmoji.setText(activityEmoji(plan.activityType));
                tvDayTitle.setText(plan.sessionTitle);
                tvDayDetail.setText(plan.sessionDetail);
                tvDayMeta.setText(intensityLabel(plan.intensityLevel)
                        + "  ·  " + plan.durationMinutes + " min"
                        + "  ·  " + plan.activityType);
                tvCoachNote.setText(plan.coachNote);
                item.setAlpha(isPast && !isToday ? 0.7f : 1f);

                // Check log status
                WorkoutLog log = logs != null ? logs.get(plan.id) : null;
                if (log != null) {
                    tvLogStatus.setVisibility(View.VISIBLE);
                    tvLogStatus.setText(logStatusLabel(log.completionStatus));
                    tvLogStatus.setTextColor(logStatusColor(log.completionStatus));
                    btnLog.setText("Edit Log");
                    btnLog.setVisibility(isToday || isPast ? View.VISIBLE : View.GONE);
                } else {
                    tvLogStatus.setVisibility(View.GONE);
                    // Only show Log button for today and past days
                    if (isToday || isPast) {
                        btnLog.setVisibility(View.VISIBLE);
                        btnLog.setText(isToday ? "Log Now →" : "Log Late");
                    } else {
                        btnLog.setVisibility(View.GONE);
                    }
                }

                // Tap log button → go to log screen
                btnLog.setOnClickListener(v -> {
                    Intent intent = new Intent(this, LogActivity.class);
                    intent.putExtra("plannedWorkoutId", plan.id);
                    intent.putExtra("activityType",    plan.activityType);
                    intent.putExtra("sessionTitle",    plan.sessionTitle);
                    intent.putExtra("sessionDetail",   plan.sessionDetail);
                    startActivity(intent);
                });
            }

            planDayContainer.addView(item);
        }
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        tvPlanSummary   = findViewById(R.id.tvPlanSummary);
        tvPlanWeekDates = findViewById(R.id.tvPlanWeekDates);
        planDayContainer = findViewById(R.id.planDayContainer);
        btnBackFromPlan  = findViewById(R.id.btnBackFromPlan);
    }

    // ── Helpers ───────────────────────────────────────────────

    private String fullDayName(String day) {
        switch (day) {
            case "MON": return "Monday";
            case "TUE": return "Tuesday";
            case "WED": return "Wednesday";
            case "THU": return "Thursday";
            case "FRI": return "Friday";
            case "SAT": return "Saturday";
            case "SUN": return "Sunday";
            default:    return day;
        }
    }

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

    private String logStatusLabel(String status) {
        switch (status) {
            case "COMPLETED": return "✅ Completed";
            case "MODIFIED":  return "✏️ Modified";
            case "SKIPPED":   return "⏭️ Skipped";
            default:          return status;
        }
    }

    private int logStatusColor(String status) {
        switch (status) {
            case "COMPLETED": return 0xFF2E7D32;
            case "MODIFIED":  return 0xFFE65100;
            case "SKIPPED":   return 0xFF9E9E9E;
            default:          return 0xFF9E9E9E;
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

    private String formatWeekDates(String weekStart) {
        // weekStart = "YYYY-MM-DD" (Monday)
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date monday = sdf.parse(weekStart);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(monday);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 6);
            java.util.Date sunday = cal.getTime();

            java.text.SimpleDateFormat display =
                    new java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault());
            return display.format(monday) + " – " + display.format(sunday);
        } catch (Exception e) {
            return weekStart;
        }
    }
}