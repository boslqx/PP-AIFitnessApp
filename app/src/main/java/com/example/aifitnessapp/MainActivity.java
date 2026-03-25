package com.example.aifitnessapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aifitnessapp.engine.WeeklyPlanManager;
import com.example.aifitnessapp.ui.auth.AuthActivity;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.util.AppExecutors;
import com.example.aifitnessapp.util.NotificationScheduler;
import com.example.aifitnessapp.util.SessionManager;

public class MainActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SessionManager    session;
    private WeeklyPlanManager weeklyPlanManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session           = new SessionManager(this);
        weeklyPlanManager = new WeeklyPlanManager(this);

        NotificationScheduler.scheduleWeeklyReminder(this);

        /*
         * ROUTING LOGIC — three possible states on app open:
         * 1. Not logged in          → AuthActivity
         * 2. Logged in, plan exists → HomeActivity
         * 3. Logged in, no plan     → weekly dialog → HomeActivity
         *
         * Session check is synchronous (SharedPreferences).
         * Plan check is async (Room on background thread).
         */
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        int userId = session.getUserId();
        if (userId == -1) {
            // Corrupted session — force re-login
            session.logout();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        checkWeeklyPlan(userId);
    }

    private void checkWeeklyPlan(int userId) {
        weeklyPlanManager.checkAndPromptIfNeeded(
                userId, mainHandler,
                new WeeklyPlanManager.WeeklyPlanCallback() {
                    @Override public void needsDialog() {
                        showWeeklyPlanDialog(userId);
                    }
                    @Override public void onComplete() { }
                    @Override public void onError(String message) {
                        Toast.makeText(MainActivity.this,
                                "⚠️ " + message, Toast.LENGTH_LONG).show();
                        goHome();
                    }
                });

        // Navigate home if plan already exists
        AppExecutors.getInstance().diskIO().execute(() -> {
            String weekStart =
                    com.example.aifitnessapp.engine.PlanEngine.getCurrentWeekStart();
            java.util.List<com.example.aifitnessapp.data.model.PlannedWorkout> existing =
                    com.example.aifitnessapp.data.db.FitAIDatabase
                            .getInstance(getApplication())
                            .plannedWorkoutDao()
                            .getWeekPlanSync(userId, weekStart);
            if (existing != null && !existing.isEmpty()) {
                mainHandler.post(this::goHome);
            }
        });
    }

    private void showWeeklyPlanDialog(int userId) {
        new AlertDialog.Builder(this)
                .setTitle("🗓 New week!")
                .setMessage(
                        "A new week has started. What would you like to do?\n\n"
                                + "🔁 New plan — AI adapts based on last week's performance.\n\n"
                                + "📋 Keep last week — same schedule, same workouts.")
                .setPositiveButton("🔁 New Plan", (dialog, which) -> {
                    showLoading();
                    weeklyPlanManager.generateNewPlan(userId, mainHandler,
                            new WeeklyPlanManager.WeeklyPlanCallback() {
                                @Override public void needsDialog() { }
                                @Override public void onComplete() {
                                    hideLoading(); goHome();
                                }
                                @Override public void onError(String msg) {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "⚠️ " + msg, Toast.LENGTH_LONG).show();
                                    goHome();
                                }
                            });
                })
                .setNegativeButton("📋 Keep Last Week", (dialog, which) -> {
                    showLoading();
                    weeklyPlanManager.copyLastWeekPlan(userId, mainHandler,
                            new WeeklyPlanManager.WeeklyPlanCallback() {
                                @Override public void needsDialog() { }
                                @Override public void onComplete() {
                                    hideLoading(); goHome();
                                }
                                @Override public void onError(String msg) {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "⚠️ " + msg, Toast.LENGTH_LONG).show();
                                    goHome();
                                }
                            });
                })
                .setCancelable(false)
                .show();
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void showLoading() {
        View pb = findViewById(R.id.pbMainLoading);
        if (pb != null) pb.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        View pb = findViewById(R.id.pbMainLoading);
        if (pb != null) pb.setVisibility(View.GONE);
    }
}