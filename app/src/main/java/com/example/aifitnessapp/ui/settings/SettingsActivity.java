package com.example.aifitnessapp.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.ui.auth.AuthActivity;
import com.example.aifitnessapp.ui.onboarding.OnboardingActivity;
import com.example.aifitnessapp.util.NotificationPreferences;
import com.example.aifitnessapp.util.NotificationScheduler;
import com.example.aifitnessapp.viewmodel.SettingsViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private SettingsViewModel viewModel;

    // ── Views ─────────────────────────────────────────────────
    private TextView     tvSettingsEmail, tvSettingsName, tvSettingsWeight;
    private TextView     tvSettingsGoal, tvSettingsActivities;
    private LinearLayout rowName, rowWeight, rowGoal, rowActivities;
    private LinearLayout rowResetPlan, rowLogout, rowClearData;
    private ProgressBar  pbSettings;
    private SwitchCompat switchDaily, switchRestDay, switchWeekly, switchStreak;
    private TextView tvNotifDailyTime;
    private NotificationPreferences notifPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        bindViews();
        setupRowClicks();
        observeViewModel();

        // Load current settings data
        viewModel.loadSettings();
    }

    // ─────────────────────────────────────────────────────────
    //  OBSERVE VIEWMODEL
    // ─────────────────────────────────────────────────────────
    private void observeViewModel() {

        loadNotificationPreferences();   // loads switches AND reschedules alarms

        // Populate rows when prefs load
        viewModel.currentPrefs.observe(this, prefs -> {
            if (prefs == null) return;
            populateRows(prefs);
        });

        // Email from session (not in UserPreferences)
        tvSettingsEmail.setText(viewModel.getEmail());

        // Save success toast
        viewModel.saveResult.observe(this, success -> {
            if (success == null || !success) return;
            Toast.makeText(this, "✅ Saved", Toast.LENGTH_SHORT).show();
            viewModel.resetSaveResult();
        });

        // Error toast
        viewModel.errorMessage.observe(this, error -> {
            if (error == null || error.isEmpty()) return;
            Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
            viewModel.resetSaveResult();
        });

        // Clear all data completed → go to onboarding (fresh start)
        viewModel.clearResult.observe(this, cleared -> {
            if (cleared == null || !cleared) return;
            Toast.makeText(this, "All data cleared.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Loading spinner
        viewModel.isLoading.observe(this, loading -> {
            pbSettings.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    // ─────────────────────────────────────────────────────────
    //  POPULATE ROWS
    // ─────────────────────────────────────────────────────────
    private void populateRows(UserPreferences prefs) {
        tvSettingsName.setText(prefs.name != null ? prefs.name : "—");
        tvSettingsWeight.setText(prefs.weightKg > 0
                ? prefs.weightKg + " kg" : "—");
        tvSettingsGoal.setText(formatGoal(prefs.fitnessGoal));
        tvSettingsActivities.setText(formatActivities(prefs.selectedActivities));
    }

    // ─────────────────────────────────────────────────────────
    //  ROW CLICK HANDLERS
    // ─────────────────────────────────────────────────────────
    private void setupRowClicks() {
        rowName.setOnClickListener(v -> showEditNameDialog());
        rowWeight.setOnClickListener(v -> showEditWeightDialog());
        rowGoal.setOnClickListener(v -> showEditGoalDialog());
        rowActivities.setOnClickListener(v -> showEditActivitiesDialog());
        rowResetPlan.setOnClickListener(v -> showResetPlanDialog());
        rowLogout.setOnClickListener(v -> showLogoutDialog());
        rowClearData.setOnClickListener(v -> showClearDataDialog());
        setupNotificationSwitches();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 1: EDIT NAME
    // ─────────────────────────────────────────────────────────
    private void showEditNameDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Your name");
        input.setPadding(48, 32, 48, 32);
        // Pre-fill with current value
        UserPreferences prefs = viewModel.currentPrefs.getValue();
        if (prefs != null && prefs.name != null) {
            input.setText(prefs.name);
            input.selectAll();
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText() != null
                            ? input.getText().toString().trim() : "";
                    if (!newName.isEmpty()) {
                        viewModel.updateName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 2: EDIT WEIGHT
    // ─────────────────────────────────────────────────────────
    private void showEditWeightDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Weight in kg");
        input.setPadding(48, 32, 48, 32);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        UserPreferences prefs = viewModel.currentPrefs.getValue();
        if (prefs != null && prefs.weightKg > 0) {
            input.setText(String.valueOf(prefs.weightKg));
            input.selectAll();
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Weight")
                .setMessage("Enter your current weight in kilograms.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        float weight = Float.parseFloat(
                                input.getText().toString().trim());
                        viewModel.updateWeight(weight);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this,
                                "Please enter a valid number.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 3: EDIT FITNESS GOAL
    // ─────────────────────────────────────────────────────────
    private void showEditGoalDialog() {
        final String[] goalKeys   = {"LOSE_FAT", "BUILD_MUSCLE",
                "ENDURANCE", "GENERAL"};
        final String[] goalLabels = {
                "🔥  Lose Fat — burn fat, get lean",
                "💪  Build Muscle — get stronger and bigger",
                "🏃  Improve Endurance — go longer, go harder",
                "⚡  General Fitness — stay healthy and active"
        };

        // Find current selection index
        UserPreferences prefs = viewModel.currentPrefs.getValue();
        int currentIndex = 0;
        if (prefs != null) {
            for (int i = 0; i < goalKeys.length; i++) {
                if (goalKeys[i].equals(prefs.fitnessGoal)) {
                    currentIndex = i;
                    break;
                }
            }
        }
        final int[] selected = {currentIndex};

        new AlertDialog.Builder(this)
                .setTitle("Fitness Goal")
                .setSingleChoiceItems(goalLabels, currentIndex,
                        (dialog, which) -> selected[0] = which)
                .setPositiveButton("Next", (dialog, which) -> {
                    String newGoal = goalKeys[selected[0]];
                    // Don't bother asking scope if nothing changed
                    if (prefs != null && newGoal.equals(prefs.fitnessGoal)) {
                        Toast.makeText(this, "No change made.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showScopeDialog(
                            applyNow -> viewModel.updateFitnessGoal(newGoal, applyNow)
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 4: EDIT ACTIVITIES
    // ─────────────────────────────────────────────────────────
    private void showEditActivitiesDialog() {
        final String[] activityKeys   = {"GYM", "RUNNING", "BOULDERING",
                "CYCLING", "SWIMMING", "YOGA",
                "HOME", "SPORTS"};
        final String[] activityLabels = {
                "🏋️  Gym", "🏃  Running / Jogging",
                "🧗  Bouldering / Climbing", "🚴  Cycling",
                "🏊  Swimming", "🧘  Yoga / Stretching",
                "🏠  Home Workouts", "⚽  Sports"
        };

        // Load current selections
        UserPreferences prefs = viewModel.currentPrefs.getValue();
        boolean[] checked = new boolean[activityKeys.length];
        if (prefs != null && prefs.selectedActivities != null) {
            for (int i = 0; i < activityKeys.length; i++) {
                checked[i] = prefs.selectedActivities.contains(activityKeys[i]);
            }
        }
        // Copy for lambda capture
        final boolean[] currentChecked = checked.clone();

        new AlertDialog.Builder(this)
                .setTitle("Activities")
                .setMultiChoiceItems(activityLabels, currentChecked,
                        (dialog, which, isChecked) -> currentChecked[which] = isChecked)
                .setPositiveButton("Next", (dialog, which) -> {
                    // Build comma-separated string from checked items
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < activityKeys.length; i++) {
                        if (currentChecked[i]) selected.add(activityKeys[i]);
                    }
                    if (selected.isEmpty()) {
                        Toast.makeText(this,
                                "Pick at least one activity.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String newActivities = String.join(",", selected);
                    showScopeDialog(
                            applyNow -> viewModel.updateActivities(newActivities, applyNow)
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  SCOPE DIALOG
    // ─────────────────────────────────────────────────────────
    interface ScopeAction {
        void execute(boolean applyNow);
    }

    private void showScopeDialog(ScopeAction action) {
        new AlertDialog.Builder(this)
                .setTitle("When to apply?")
                .setItems(new String[]{
                        "📅  This week — regenerate current plan now",
                        "⏭️  Next week only — keep this week as-is"
                }, (dialog, which) -> {
                    boolean applyNow = (which == 0);
                    action.execute(applyNow);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  RESET PLAN DIALOG
    // ─────────────────────────────────────────────────────────
    private void showResetPlanDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset This Week's Plan?")
                .setMessage(
                        "This will delete your current week's plan and generate "
                                + "a completely fresh one based on your profile.\n\n"
                                + "Any edits you made to individual days will be lost.")
                .setPositiveButton("Reset Plan", (dialog, which) ->
                        viewModel.resetPlan())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  LOG OUT DIALOG
    // ─────────────────────────────────────────────────────────
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out?")
                .setMessage("You'll need to log back in next time.\n"
                        + "Your data will be kept.")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    viewModel.logout();
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  CLEAR ALL DATA DIALOG — double confirmation
    // ─────────────────────────────────────────────────────────
    private void showClearDataDialog() {
        // Step 1 — first warning
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Clear All Data?")
                .setMessage(
                        "This will permanently delete:\n\n"
                                + "• Your account\n"
                                + "• All workout logs\n"
                                + "• Your fitness plan\n"
                                + "• All settings\n\n"
                                + "This cannot be undone.")
                .setPositiveButton("Continue", (d1, w1) -> {
                    // Step 2 — second confirmation, more direct wording
                    new AlertDialog.Builder(this)
                            .setTitle("Are you absolutely sure?")
                            .setMessage("All your data will be deleted permanently. "
                                    + "You will start from scratch.")
                            .setPositiveButton("Yes, Delete Everything",
                                    (d2, w2) -> viewModel.clearAllData())
                            .setNegativeButton("No, Keep My Data", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── View binding ──────────────────────────────────────────
    private void bindViews() {
        tvSettingsEmail      = findViewById(R.id.tvSettingsEmail);
        tvSettingsName       = findViewById(R.id.tvSettingsName);
        tvSettingsWeight     = findViewById(R.id.tvSettingsWeight);
        tvSettingsGoal       = findViewById(R.id.tvSettingsGoal);
        tvSettingsActivities = findViewById(R.id.tvSettingsActivities);
        rowName              = findViewById(R.id.rowSettingsName);
        rowWeight            = findViewById(R.id.rowSettingsWeight);
        rowGoal              = findViewById(R.id.rowSettingsGoal);
        rowActivities        = findViewById(R.id.rowSettingsActivities);
        rowResetPlan         = findViewById(R.id.rowSettingsResetPlan);
        rowLogout            = findViewById(R.id.rowSettingsLogout);
        rowClearData         = findViewById(R.id.rowSettingsClearData);
        pbSettings           = findViewById(R.id.pbSettings);
        switchDaily       = findViewById(R.id.switchNotifDaily);
        switchRestDay     = findViewById(R.id.switchNotifRestDay);
        switchWeekly      = findViewById(R.id.switchNotifWeekly);
        switchStreak      = findViewById(R.id.switchNotifStreak);
        tvNotifDailyTime  = findViewById(R.id.tvNotifDailyTime);
        notifPrefs        = new NotificationPreferences(this);
    }

    // ── Display helpers ───────────────────────────────────────
    private String formatGoal(String goal) {
        if (goal == null) return "—";
        switch (goal) {
            case "LOSE_FAT":      return "🔥 Lose Fat";
            case "BUILD_MUSCLE":  return "💪 Build Muscle";
            case "ENDURANCE":     return "🏃 Endurance";
            case "GENERAL":       return "⚡ General Fitness";
            default:              return goal;
        }
    }

    private String formatActivities(String activitiesStr) {
        if (activitiesStr == null || activitiesStr.isEmpty()) return "—";
        String[] parts = activitiesStr.split(",");
        List<String> labels = new ArrayList<>();
        for (String part : parts) {
            labels.add(formatSingleActivity(part.trim()));
        }
        if (labels.size() <= 2) {
            return String.join(", ", labels);
        }
        return labels.get(0) + ", " + labels.get(1)
                + " +" + (labels.size() - 2) + " more";
    }

    private String formatSingleActivity(String type) {
        switch (type) {
            case "GYM":        return "Gym";
            case "RUNNING":    return "Running";
            case "BOULDERING": return "Bouldering";
            case "CYCLING":    return "Cycling";
            case "SWIMMING":   return "Swimming";
            case "YOGA":       return "Yoga";
            case "HOME":       return "Home";
            case "SPORTS":     return "Sports";
            default:           return type;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  NOTIFICATION METHODS (FIXED)
    // ─────────────────────────────────────────────────────────

    /*
     * Loads saved notification preferences and sets switch states.
     * Then reschedules any enabled alarms to ensure they are active
     * after app restart or device reboot.
     */
    private void loadNotificationPreferences() {
        // Set switch states from saved preferences
        switchDaily.setChecked(notifPrefs.isDailyEnabled());
        switchRestDay.setChecked(notifPrefs.isRestDayEnabled());
        switchWeekly.setChecked(notifPrefs.isWeeklyEnabled());
        switchStreak.setChecked(notifPrefs.isStreakEnabled());

        // Update the daily time label
        updateDailyTimeLabel();

        // Reschedule all enabled alarms (restores state after process death/reboot)
        if (notifPrefs.isDailyEnabled()) {
            NotificationScheduler.scheduleDailyReminder(this,
                    notifPrefs.getDailyHour(), notifPrefs.getDailyMinute());
        }
        if (notifPrefs.isRestDayEnabled()) {
            NotificationScheduler.scheduleRestDayReminder(this);
        }
        if (notifPrefs.isWeeklyEnabled()) {
            NotificationScheduler.scheduleWeeklyReminder(this);
        }
        if (notifPrefs.isStreakEnabled()) {
            NotificationScheduler.scheduleStreakReminder(this);
        }
    }

    /*
     * Attaches listeners to all notification switches.
     * Daily switch now uses a callback to handle user cancellation properly.
     */
    private void setupNotificationSwitches() {

        // ── Daily reminder switch ─────────────────────────────
        switchDaily.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                // Show time picker; only enable if user confirms
                showDailyTimePickerWithCallback(
                        () -> {
                            // Success: save preference (time already saved in picker)
                            notifPrefs.setDailyEnabled(true);
                        },
                        () -> {
                            // Cancel: revert switch and preference
                            switchDaily.setChecked(false);
                            notifPrefs.setDailyEnabled(false);
                            tvNotifDailyTime.setText("Off");
                        }
                );
            } else {
                // Turned off explicitly
                notifPrefs.setDailyEnabled(false);
                NotificationScheduler.cancelDailyReminder(this);
                tvNotifDailyTime.setText("Off");
            }
        });

        // Tap the daily row (not just the switch) to change time
        findViewById(R.id.rowNotifDaily).setOnClickListener(v -> {
            if (notifPrefs.isDailyEnabled()) {
                showDailyTimePickerWithCallback(
                        () -> {
                            // already saved by picker
                        },
                        null // no special cancel action needed when manually opening picker
                );
            } else {
                switchDaily.setChecked(true); // turning on triggers the listener above
            }
        });

        // ── Rest day switch ───────────────────────────────────
        switchRestDay.setOnCheckedChangeListener((btn, isChecked) -> {
            notifPrefs.setRestDayEnabled(isChecked);
            if (isChecked) {
                NotificationScheduler.scheduleRestDayReminder(this);
            } else {
                NotificationScheduler.cancelRestDayReminder(this);
            }
        });

        // ── Weekly switch ─────────────────────────────────────
        switchWeekly.setOnCheckedChangeListener((btn, isChecked) -> {
            notifPrefs.setWeeklyEnabled(isChecked);
            if (isChecked) {
                NotificationScheduler.scheduleWeeklyReminder(this);
            } else {
                NotificationScheduler.cancelWeeklyReminder(this);
            }
        });

        // ── Streak switch ─────────────────────────────────────
        switchStreak.setOnCheckedChangeListener((btn, isChecked) -> {
            notifPrefs.setStreakEnabled(isChecked);
            if (isChecked) {
                NotificationScheduler.scheduleStreakReminder(this);
            } else {
                NotificationScheduler.cancelStreakReminder(this);
            }
        });
    }

    /*
     * Shows time picker with callbacks for success and cancellation.
     * @param onSuccess called when user picks a time (time is already saved)
     * @param onCancel called if the user dismisses the dialog without picking
     */
    private void showDailyTimePickerWithCallback(Runnable onSuccess, Runnable onCancel) {
        int currentHour   = notifPrefs.getDailyHour();
        int currentMinute = notifPrefs.getDailyMinute();

        android.app.TimePickerDialog picker = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    // Save new time
                    notifPrefs.setDailyTime(hourOfDay, minute);
                    // Reschedule alarm at new time
                    NotificationScheduler.scheduleDailyReminder(this, hourOfDay, minute);
                    // Update label
                    updateDailyTimeLabel();
                    if (onSuccess != null) onSuccess.run();
                },
                currentHour,
                currentMinute,
                false // 12-hour format with AM/PM
        );
        picker.setTitle("Reminder time");
        picker.setOnCancelListener(dialog -> {
            if (onCancel != null) onCancel.run();
        });
        picker.show();
    }

    private void updateDailyTimeLabel() {
        if (notifPrefs.isDailyEnabled()) {
            tvNotifDailyTime.setText(notifPrefs.getDailyTimeLabel());
        } else {
            tvNotifDailyTime.setText("Off");
        }
    }
}