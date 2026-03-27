package com.example.aifitnessapp.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.ui.coach.CoachActivity;
import com.example.aifitnessapp.ui.log.LogActivity;
import com.example.aifitnessapp.ui.plan.PlanActivity;
import com.example.aifitnessapp.ui.progress.ProgressActivity;
import com.example.aifitnessapp.ui.settings.SettingsActivity;
import com.example.aifitnessapp.util.NotificationPreferences;
import com.example.aifitnessapp.util.NotificationScheduler;
import com.example.aifitnessapp.viewmodel.HomeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;

    // Header
    private TextView tvGreeting, tvWeekLabel;

    // Week strip (inside card)
    private LinearLayout weekStrip;

    // Today card
    private MaterialCardView cardToday;
    private TextView tvTodayEmoji, tvTodayTitle, tvTodayDetail;
    private TextView tvTodayIntensity, tvTodayDuration, tvTodayCoachNote;
    private MaterialButton btnStartWorkout, btnRestDay;
    private TextView tvAlreadyLogged;

    // Feed
    private LinearLayout feedContainer;
    private TextView tvFeedLabel;

    // Bottom nav
    private MaterialButton btnNavLog, btnNavPlan, btnNavProgress, btnNavCoach, btnNavSettings;

    private int currentUserId = -1;
    private ActivityResultLauncher<String> notifPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Notification permission handling (must be called before any notification scheduling)
        registerNotificationPermissionLauncher();
        requestNotificationPermissionIfNeeded();

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        bindViews();
        setupBottomNav();

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            currentUserId = user.id;
            tvGreeting.setText(viewModel.getGreeting(user.name));
            viewModel.loadWeekLabel(user.id);

            // Only trigger initPlan once — guard with a flag
            if (!viewModel.planReady.getValue()) {
                viewModel.initPlan(user.id);
            } else {
                // Already ready (e.g. rotation) — just load feed
                viewModel.loadFeed(user.id);
            }
        });

        viewModel.coachMessage.observe(this, msg ->
                tvWeekLabel.setText(msg));

        // planReady: only act when it actually flips to true
        viewModel.planReady.observe(this, ready -> {
            if (!ready || currentUserId == -1) return;
            viewModel.loadPlan(currentUserId);
            viewModel.loadFeed(currentUserId);
            observePlan();
        });

        viewModel.feedItems.observe(this, items -> {
            if (items == null) return;
            buildFeed(items);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only refresh feed if plan is already loaded (guards against first launch)
        if (currentUserId != -1
                && viewModel.planReady.getValue() != null
                && viewModel.planReady.getValue()
                && viewModel.todayPlan != null) {
            viewModel.loadFeed(currentUserId);
        }
    }

    private void observePlan() {
        viewModel.todayPlan.observe(this, plan -> {
            if (plan == null) return;
            renderTodayCard(plan);
        });

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
            tvTodayDuration.setVisibility(View.GONE);
            btnStartWorkout.setVisibility(View.GONE);
            btnRestDay.setVisibility(View.VISIBLE);
        } else {
            tvTodayIntensity.setText(intensityLabel(plan.intensityLevel)
                    + "  ·  " + plan.durationMinutes + " min");
            tvTodayDuration.setVisibility(View.GONE);
            btnStartWorkout.setVisibility(View.VISIBLE);
            btnRestDay.setVisibility(View.GONE);
        }

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

        btnStartWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogActivity.class);
            intent.putExtra("plannedWorkoutId", plan.id);
            intent.putExtra("activityType",     plan.activityType);
            intent.putExtra("sessionTitle",     plan.sessionTitle);
            intent.putExtra("sessionDetail",    plan.sessionDetail);
            startActivity(intent);
        });

        btnRestDay.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogActivity.class);
            intent.putExtra("plannedWorkoutId", plan.id);
            intent.putExtra("activityType",     "REST");
            intent.putExtra("sessionTitle",     "Rest Day");
            intent.putExtra("sessionDetail",    "");
            startActivity(intent);
        });
    }

    // ── Week strip ────────────────────────────────────────────

    private void renderWeekStrip(List<PlannedWorkout> plans) {
        weekStrip.removeAllViews();
        String today = PlanEngine.getTodayDayOfWeek();

        for (PlannedWorkout plan : plans) {
            View dayView = LayoutInflater.from(this)
                    .inflate(R.layout.item_week_day, weekStrip, false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dayView.setLayoutParams(params);

            TextView tvDayLabel = dayView.findViewById(R.id.tvDayLabel);
            TextView tvDayIcon  = dayView.findViewById(R.id.tvDayIcon);
            View     indicator  = dayView.findViewById(R.id.todayIndicator);

            if (tvDayLabel == null || tvDayIcon == null || indicator == null) continue;

            tvDayLabel.setText(plan.dayOfWeek.substring(0, 1)
                    + plan.dayOfWeek.substring(1, 2).toLowerCase());
            tvDayIcon.setText(plan.isRestDay ? "😴" : activityEmoji(plan.activityType));

            boolean isToday  = plan.dayOfWeek.equals(today);
            boolean isPast   = isDayPast(plan.dayOfWeek, today);
            boolean isFuture = !isToday && !isPast;

            indicator.setVisibility(isToday ? View.VISIBLE : View.INVISIBLE);

            if (isToday) {
                tvDayLabel.setTextColor(getColor(R.color.md_theme_primary));
                tvDayIcon.setAlpha(1f);
            } else if (isPast) {
                tvDayLabel.setTextColor(0xFF9E9E9E);
                tvDayIcon.setAlpha(0.9f);
            } else {
                // Future days — faded
                tvDayLabel.setTextColor(0xFFCCCCCC);
                tvDayIcon.setAlpha(0.35f);
            }

            weekStrip.addView(dayView);
        }
    }

    // ── Feed ──────────────────────────────────────────────────

    private void buildFeed(List<HomeViewModel.FeedItem> items) {
        feedContainer.removeAllViews();

        if (items.isEmpty()) {
            tvFeedLabel.setVisibility(View.GONE);
            return;
        }

        tvFeedLabel.setVisibility(View.VISIBLE);

        for (HomeViewModel.FeedItem item : items) {
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_feed_card, feedContainer, false);

            ImageView ivPhoto  = card.findViewById(R.id.ivFeedPhoto);
            TextView  tvEmoji  = card.findViewById(R.id.tvFeedEmoji);
            TextView  tvTitle  = card.findViewById(R.id.tvFeedTitle);
            TextView  tvDate   = card.findViewById(R.id.tvFeedDate);
            TextView  tvStatus = card.findViewById(R.id.tvFeedStatus);
            TextView  tvEffort = card.findViewById(R.id.tvFeedEffort);
            LinearLayout exContainer = card.findViewById(R.id.feedExerciseContainer);
            TextView  tvMore   = card.findViewById(R.id.tvFeedMoreExercises);

            // Photo
            if (item.log.photoPath != null && !item.log.photoPath.isEmpty()) {
                ivPhoto.setVisibility(View.VISIBLE);
                ivPhoto.setImageURI(Uri.parse(item.log.photoPath));
            } else {
                ivPhoto.setVisibility(View.GONE);
            }

            // Header
            tvEmoji.setText(activityEmoji(item.activityType));
            tvTitle.setText(item.sessionTitle);
            tvDate.setText(item.displayDate);
            tvStatus.setText(statusEmoji(item.log.completionStatus));

            // Effort
            if (item.log.perceivedEffort > 0) {
                tvEffort.setText(effortDots(item.log.perceivedEffort)
                        + "  " + effortLabel(item.log.perceivedEffort));
                tvEffort.setVisibility(View.VISIBLE);
            } else {
                tvEffort.setVisibility(View.GONE);
            }

            // Exercises — parse from notes, show max 3
            if (item.log.notes != null && !item.log.notes.isEmpty()) {
                String[] lines = item.log.notes.split("\n");
                int shown = 0;
                int total = 0;

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("✅") || line.startsWith("⏭️")) total++;
                }

                for (String line : lines) {
                    line = line.trim();
                    if (!line.startsWith("✅") && !line.startsWith("⏭️")) continue;
                    if (shown >= 3) break;

                    TextView tv = new TextView(this);
                    tv.setText(line);
                    tv.setTextSize(12f);
                    tv.setTextColor(line.startsWith("✅") ? 0xFF2E7D32 : 0xFF9E9E9E);
                    tv.setPadding(0, 2, 0, 2);
                    exContainer.addView(tv);
                    shown++;
                }

                int overflow = total - shown;
                if (overflow > 0) {
                    tvMore.setText("+ " + overflow + " more");
                    tvMore.setVisibility(View.VISIBLE);
                }
            }

            // Tap → detail screen
            WorkoutLog log  = item.log;
            PlannedWorkout plan = item.plan;
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, WorkoutDetailActivity.class);
                intent.putExtra("date",             item.displayDate);
                intent.putExtra("sessionTitle",     item.sessionTitle);
                intent.putExtra("activityType",     item.activityType);
                intent.putExtra("completionStatus", log.completionStatus);
                intent.putExtra("perceivedEffort",  log.perceivedEffort);
                intent.putExtra("notes",            log.notes);
                intent.putExtra("photoPath",        log.photoPath);
                intent.putExtra("durationSeconds",  log.durationSeconds);
                intent.putExtra("coachNote",
                        plan != null ? plan.coachNote : "");
                startActivity(intent);
            });

            // Skipped posts — slightly faded
            if ("SKIPPED".equals(item.log.completionStatus)) {
                card.setAlpha(0.6f);
            }

            feedContainer.addView(card);
        }
    }

    // ── Bottom nav ────────────────────────────────────────────

    private void setupBottomNav() {
        btnNavLog.setOnClickListener(v ->
                startActivity(new Intent(this, LogActivity.class)));
        btnNavPlan.setOnClickListener(v ->
                startActivity(new Intent(this, PlanActivity.class)));
        btnNavProgress.setOnClickListener(v ->
                startActivity(new Intent(this, ProgressActivity.class)));
        btnNavCoach.setOnClickListener(v ->
                startActivity(new Intent(this, CoachActivity.class)));
        btnNavSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        tvGreeting       = findViewById(R.id.tvGreeting);
        tvWeekLabel      = findViewById(R.id.tvWeekLabel);
        weekStrip        = findViewById(R.id.weekStrip);
        cardToday        = findViewById(R.id.cardToday);
        tvTodayEmoji     = findViewById(R.id.tvTodayEmoji);
        tvTodayTitle     = findViewById(R.id.tvTodayTitle);
        tvTodayDetail    = findViewById(R.id.tvTodayDetail);
        tvTodayIntensity = findViewById(R.id.tvTodayIntensity);
        tvTodayDuration  = findViewById(R.id.tvTodayDuration);
        tvTodayCoachNote = findViewById(R.id.tvTodayCoachNote);
        btnStartWorkout  = findViewById(R.id.btnStartWorkout);
        btnRestDay       = findViewById(R.id.btnRestDay);
        tvAlreadyLogged  = findViewById(R.id.tvAlreadyLogged);
        feedContainer    = findViewById(R.id.feedContainer);
        tvFeedLabel      = findViewById(R.id.tvFeedLabel);
        btnNavLog        = findViewById(R.id.btnNavLog);
        btnNavPlan       = findViewById(R.id.btnNavPlan);
        btnNavProgress   = findViewById(R.id.btnNavProgress);
        btnNavCoach      = findViewById(R.id.btnNavCoach);
        btnNavSettings = findViewById(R.id.btnNavSettings);
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

    private String statusEmoji(String s) {
        if (s == null) return "—";
        switch (s) {
            case "COMPLETED": return "✅";
            case "MODIFIED":  return "✏️";
            case "SKIPPED":   return "⏭️";
            default:          return "—";
        }
    }

    private String effortDots(int e) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= e ? "●" : "○");
        return sb.toString();
    }

    private String effortLabel(int e) {
        switch (e) {
            case 1: return "Very Easy";
            case 2: return "Light";
            case 3: return "Moderate";
            case 4: return "Hard";
            case 5: return "Very Hard";
            default: return "";
        }
    }

    private boolean isDayPast(String day, String today) {
        String[] order = {"MON","TUE","WED","THU","FRI","SAT","SUN"};
        int d = 0, t = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(day))   d = i;
            if (order[i].equals(today)) t = i;
        }
        return d < t;
    }

    // ── Notification permission handling (Android 13+) ───────

    /*
     * registerForActivityResult() MUST be called before onCreate completes
     * (before the Activity starts). That's why we register in a separate
     * method called at the top of onCreate, not inside a click handler.
     *
     * The ActivityResultLauncher replaces the old startActivityForResult()
     * pattern. When the user responds to the permission dialog, the lambda
     * receives `granted` (true/false).
     */
    private void registerNotificationPermissionLauncher() {
        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    // Mark as asked regardless of result so we don't ask again
                    new NotificationPreferences(this).markPermissionAsked();
                    if (granted) {
                        // Permission granted — schedule default notifications
                        NotificationScheduler.scheduleAllFromPreferences(this);
                    }
                    // If denied, notifications simply won't fire. User can
                    // enable them later via device Settings. We don't pester.
                });
    }

    /*
     * Requests POST_NOTIFICATIONS permission on Android 13+ (API 33+).
     * On older versions the permission is granted automatically — skip.
     *
     * Only asks ONCE — after that, NotificationPreferences.wasPermissionAsked()
     * returns true and we never show the dialog again.
     *
     * If already granted (user approved before), just schedule directly.
     */
    private void requestNotificationPermissionIfNeeded() {
        // Only needed on Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // On older Android, just schedule everything
            NotificationScheduler.scheduleAllFromPreferences(this);
            return;
        }

        NotificationPreferences notifPrefs = new NotificationPreferences(this);

        // Already asked before — don't ask again
        if (notifPrefs.wasPermissionAsked()) {
            // If they granted it before, reschedule in case of reboot
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationScheduler.scheduleAllFromPreferences(this);
            }
            return;
        }

        // First time — check current status
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            // Already granted (shouldn't happen on first open but handle it)
            notifPrefs.markPermissionAsked();
            NotificationScheduler.scheduleAllFromPreferences(this);
        } else {
            // Launch the system permission dialog
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}