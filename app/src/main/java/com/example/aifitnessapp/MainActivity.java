package com.example.aifitnessapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.engine.WeeklyPlanManager;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.ui.onboarding.OnboardingActivity;
import com.example.aifitnessapp.util.AppExecutors;
import com.example.aifitnessapp.util.NotificationScheduler;

public class MainActivity extends AppCompatActivity {

    /*
     * Handler bound to the MAIN thread's Looper.
     * Used to post UI work (showing dialogs, starting activities)
     * from background threads back to the main thread.
     *
     * WHY Looper.getMainLooper()?
     * Every thread has a Looper — a loop that processes messages.
     * The main (UI) thread's Looper is special: only it can update views.
     * new Handler(Looper.getMainLooper()) creates a handler that always
     * posts work to the main thread's message queue.
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WeeklyPlanManager weeklyPlanManager;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weeklyPlanManager = new WeeklyPlanManager(this);

        // Schedule the weekly Monday notification (idempotent — safe to call every open)
        NotificationScheduler.scheduleWeeklyReminder(this);

        // Step 1: Check if user has completed onboarding
        AppExecutors.getInstance().diskIO().execute(() -> {
            int count = FitAIDatabase.getInstance(getApplication())
                    .userPreferencesDao().getUserCount();

            if (count == 0) {
                // No user — go to onboarding
                mainHandler.post(() -> {
                    startActivity(new Intent(this, OnboardingActivity.class));
                    finish();
                });
                return;
            }

            // User exists — get their ID for the weekly check
            int userId = FitAIDatabase.getInstance(getApplication())
                    .userPreferencesDao().getCurrentUserSync().id;

            mainHandler.post(() -> {
                currentUserId = userId;
                // Step 2: Check if a weekly plan is needed
                checkWeeklyPlan(userId);
            });
        });
    }

    // ─────────────────────────────────────────────────────────
    //  WEEKLY PLAN CHECK
    //
    //  Asks WeeklyPlanManager if this week has a plan.
    //  If not, shows the "Keep last week or new plan?" dialog.
    //  If yes, goes straight to HomeActivity.
    // ─────────────────────────────────────────────────────────
    private void checkWeeklyPlan(int userId) {
        weeklyPlanManager.checkAndPromptIfNeeded(
                userId,
                mainHandler,
                new WeeklyPlanManager.WeeklyPlanCallback() {

                    @Override
                    public void needsDialog() {
                        // Called on main thread — safe to show dialog
                        showWeeklyPlanDialog(userId);
                    }

                    @Override
                    public void onComplete() {
                        // Plan was already there — go straight home
                        goHome();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(MainActivity.this,
                                "⚠️ " + message, Toast.LENGTH_LONG).show();
                        goHome(); // navigate anyway so user isn't stuck
                    }
                }
        );

        // If plan exists, checkAndPromptIfNeeded calls nothing back —
        // we need to go home. We handle this by observing the "plan exists"
        // case: if needsDialog() is never called, we navigate after a check.
        // Actually WeeklyPlanManager handles this: if plan exists it does nothing,
        // so we must also handle the "plan exists" path here.
        // We do this by adding a separate sync check on the background thread.
        AppExecutors.getInstance().diskIO().execute(() -> {
            String weekStart = com.example.aifitnessapp.engine.PlanEngine
                    .getCurrentWeekStart();
            java.util.List<com.example.aifitnessapp.data.model.PlannedWorkout> existing =
                    FitAIDatabase.getInstance(getApplication())
                            .plannedWorkoutDao()
                            .getWeekPlanSync(userId, weekStart);

            boolean planExists = existing != null && !existing.isEmpty();
            if (planExists) {
                mainHandler.post(this::goHome);
            }
            // If plan is missing, needsDialog() will be called by the manager
        });
    }

    // ─────────────────────────────────────────────────────────
    //  WEEKLY PLAN DIALOG
    //
    //  Two choices:
    //  1. "🔁 New plan" — AI regenerates based on last week's logs
    //  2. "📋 Keep last week" — copies last week's workouts to this week
    //
    //  The dialog is NOT cancellable (setCancelable false) because the
    //  user MUST choose — they can't go home without a plan for the week.
    //
    //  Both buttons show a loading state while the background work runs,
    //  then navigate to HomeActivity when done.
    // ─────────────────────────────────────────────────────────
    private void showWeeklyPlanDialog(int userId) {
        new AlertDialog.Builder(this)
                .setTitle("🗓 New week!")
                .setMessage(
                        "A new week has started. What would you like to do?\n\n"
                                + "🔁 New plan — AI adapts based on last week's performance.\n\n"
                                + "📋 Keep last week — same schedule, same workouts.")
                // Positive = New plan (AI-generated)
                .setPositiveButton("🔁 New Plan", (dialog, which) -> {
                    showLoading();
                    weeklyPlanManager.generateNewPlan(
                            userId, mainHandler,
                            new WeeklyPlanManager.WeeklyPlanCallback() {
                                @Override public void needsDialog() { }
                                @Override public void onComplete() {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "✅ New plan ready!",
                                            Toast.LENGTH_SHORT).show();
                                    goHome();
                                }
                                @Override public void onError(String msg) {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "⚠️ " + msg,
                                            Toast.LENGTH_LONG).show();
                                    goHome();
                                }
                            });
                })
                // Negative = Keep last week (copy)
                .setNegativeButton("📋 Keep Last Week", (dialog, which) -> {
                    showLoading();
                    weeklyPlanManager.copyLastWeekPlan(
                            userId, mainHandler,
                            new WeeklyPlanManager.WeeklyPlanCallback() {
                                @Override public void needsDialog() { }
                                @Override public void onComplete() {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "✅ Last week's plan copied!",
                                            Toast.LENGTH_SHORT).show();
                                    goHome();
                                }
                                @Override public void onError(String msg) {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "⚠️ " + msg,
                                            Toast.LENGTH_LONG).show();
                                    goHome();
                                }
                            });
                })
                // Not cancellable — user must choose
                .setCancelable(false)
                .show();
    }

    // ── Navigation ────────────────────────────────────────────

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    // ── Loading state helpers ─────────────────────────────────
    /*
     * Shows a simple ProgressBar while background work runs.
     * activity_main.xml already has a ProgressBar — we just show/hide it.
     * This prevents the user from tapping things mid-save.
     */
    private void showLoading() {
        View pb = findViewById(R.id.pbMainLoading);
        if (pb != null) pb.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        View pb = findViewById(R.id.pbMainLoading);
        if (pb != null) pb.setVisibility(View.GONE);
    }
}