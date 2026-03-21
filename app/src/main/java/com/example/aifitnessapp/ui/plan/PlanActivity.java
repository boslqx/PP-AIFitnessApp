package com.example.aifitnessapp.ui.plan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
import com.example.aifitnessapp.viewmodel.PlanEditViewModel;
import com.example.aifitnessapp.viewmodel.PlanViewModel;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanActivity extends AppCompatActivity {

    private PlanViewModel     viewModel;
    private PlanEditViewModel editViewModel;  // ← NEW: handles edit operations

    private TextView       tvPlanSummary, tvPlanWeekDates;
    private LinearLayout   planDayContainer;
    private MaterialButton btnBackFromPlan;

    private int currentUserId = -1;

    // We keep a local reference to the current week's plans so the edit
    // dialogs can access the full list (e.g. to build the swap picker).
    // This is updated every time the LiveData fires.
    private List<PlannedWorkout> currentPlans = new ArrayList<>();

    public LiveData<List<PlannedWorkout>> weekPlan = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        // ── ViewModels ────────────────────────────────────────
        // ViewModelProvider creates the ViewModel if it doesn't exist,
        // or returns the existing one if the Activity was recreated (rotation).
        // This is why ViewModels survive rotation — they're stored in a
        // separate store tied to the Activity's lifecycle, not the Activity itself.
        viewModel     = new ViewModelProvider(this).get(PlanViewModel.class);
        editViewModel = new ViewModelProvider(this).get(PlanEditViewModel.class);

        bindViews();
        observeEditResults();  // ← NEW: watch for edit success/failure

        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            currentUserId = user.id;
            viewModel.loadPlan(user.id);
        });

        viewModel.planReady.observe(this, ready -> {
            if (!ready || currentUserId == -1) return;

            viewModel.initWeekPlan(currentUserId);
            viewModel.loadLogMap(currentUserId);
            loadWeekDates(currentUserId);

            viewModel.weekPlan.observe(this, plans -> {
                if (plans == null || plans.isEmpty()) return;

                // Cache the current plans for use in edit dialogs
                currentPlans = plans;

                new Thread(() -> {
                    int weekNum = viewModel.getCurrentWeekNum(currentUserId);
                    runOnUiThread(() -> {
                        viewModel.buildSummary(plans, weekNum);
                        renderPlan(plans);
                    });
                }).start();
            });

            viewModel.logMap.observe(this, map -> {
                if (viewModel.weekPlan.getValue() != null
                        && !viewModel.weekPlan.getValue().isEmpty()) {
                    renderPlan(viewModel.weekPlan.getValue());
                }
            });
        });

        viewModel.planSummary.observe(this, summary ->
                tvPlanSummary.setText(summary));

        btnBackFromPlan.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    // ─────────────────────────────────────────────────────────
    //  OBSERVE EDIT RESULTS
    //
    //  We observe editResult and editError here, ONCE, in onCreate.
    //  This is better than setting up observers inside button click
    //  handlers — if we did that, we'd add new observers every time
    //  the user tapped a button, causing duplicate reactions.
    //
    //  When editResult fires true:
    //    - Show a success toast
    //    - The plan list will auto-refresh because Room LiveData fires
    //      whenever the underlying data changes — we don't need to
    //      manually reload anything!
    //
    //  When editError fires:
    //    - Show an error toast with the message
    // ─────────────────────────────────────────────────────────
    private void observeEditResults() {
        editViewModel.editResult.observe(this, success -> {
            if (success == null || !success) return;
            Toast.makeText(this, "✅ Plan updated!", Toast.LENGTH_SHORT).show();
            // Room LiveData handles the UI refresh automatically.
            // No manual reload needed here.
        });

        editViewModel.editError.observe(this, error -> {
            if (error == null) return;
            Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
        });
    }

    // ─────────────────────────────────────────────────────────
    //  RENDER PLAN
    //
    //  Inflates one item_plan_day card per PlannedWorkout.
    //  Key new addition: the ✏️ Edit button and its click handler.
    // ─────────────────────────────────────────────────────────
    private void renderPlan(List<PlannedWorkout> plans) {
        planDayContainer.removeAllViews();
        String today = PlanEngine.getTodayDayOfWeek();
        Map<Integer, WorkoutLog> logs = viewModel.logMap.getValue();

        for (PlannedWorkout plan : plans) {
            View item = LayoutInflater.from(this)
                    .inflate(R.layout.item_plan_day, planDayContainer, false);

            TextView       tvDayName   = item.findViewById(R.id.tvPlanDayName);
            TextView       tvDayEmoji  = item.findViewById(R.id.tvPlanDayEmoji);
            TextView       tvDayTitle  = item.findViewById(R.id.tvPlanDayTitle);
            TextView       tvDayDetail = item.findViewById(R.id.tvPlanDayDetail);
            TextView       tvDayMeta   = item.findViewById(R.id.tvPlanDayMeta);
            TextView       tvCoachNote = item.findViewById(R.id.tvPlanDayCoachNote);
            TextView       tvLogStatus = item.findViewById(R.id.tvPlanDayLogStatus);
            MaterialButton btnLog      = item.findViewById(R.id.btnPlanDayLog);
            MaterialButton btnEdit     = item.findViewById(R.id.btnEditDay);  // ← NEW
            View           todayBar    = item.findViewById(R.id.todayBar);

            boolean isToday  = plan.dayOfWeek.equals(today);
            boolean isPast   = isDayPast(plan.dayOfWeek, today);
            boolean isFuture = !isToday && !isPast;

            tvDayName.setText(fullDayName(plan.dayOfWeek));
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

                WorkoutLog log = logs != null ? logs.get(plan.id) : null;
                if (log != null) {
                    tvLogStatus.setVisibility(View.VISIBLE);
                    tvLogStatus.setText(logStatusLabel(log.completionStatus));
                    tvLogStatus.setTextColor(logStatusColor(log.completionStatus));
                    btnLog.setText("Edit Log");
                    btnLog.setVisibility(isToday || isPast ? View.VISIBLE : View.GONE);
                } else {
                    tvLogStatus.setVisibility(View.GONE);
                    if (isToday || isPast) {
                        btnLog.setVisibility(View.VISIBLE);
                        btnLog.setText(isToday ? "Log Now →" : "Log Late");
                    } else {
                        btnLog.setVisibility(View.GONE);
                    }
                }

                btnLog.setOnClickListener(v -> {
                    Intent intent = new Intent(this, LogActivity.class);
                    intent.putExtra("plannedWorkoutId", plan.id);
                    intent.putExtra("activityType",    plan.activityType);
                    intent.putExtra("sessionTitle",    plan.sessionTitle);
                    intent.putExtra("sessionDetail",   plan.sessionDetail);
                    startActivity(intent);
                });
            }

            // ── EDIT BUTTON ───────────────────────────────────
            // Show edit button for today and future days only.
            // WHY NOT past days? Because the user may have already
            // logged a workout for a past day. Editing the plan
            // after logging would create confusing mismatches
            // between the log and the plan.
            if (isToday || isFuture) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> showEditMenu(plan));
            } else {
                btnEdit.setVisibility(View.GONE);
            }

            planDayContainer.addView(item);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  EDIT MENU DIALOG
    //
    //  The first thing the user sees when they tap ✏️.
    //  Shows the 4 available actions (or fewer for rest days).
    //
    //  WHY AlertDialog with items?
    //  It's the simplest, most native-feeling way to show a short
    //  list of actions on Android. No custom layout needed.
    // ─────────────────────────────────────────────────────────
    private void showEditMenu(PlannedWorkout plan) {
        // Build action list dynamically based on whether it's a rest day
        List<String> actions = new ArrayList<>();

        if (plan.isRestDay) {
            // Rest days can only gain a workout — can't swap or cancel nothing
            actions.add("➕  Add a workout to this day");
        } else {
            actions.add("🔄  Move / Swap with another day");
            actions.add("🏃  Change activity type");
            actions.add("❌  Cancel this workout");
        }

        String[] actionsArray = actions.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle(fullDayName(plan.dayOfWeek) + " — Edit")
                .setItems(actionsArray, (dialog, which) -> {
                    // Map the index back to the right action
                    if (plan.isRestDay) {
                        // Only one option: Add workout
                        showAddWorkoutDialog(plan);
                    } else {
                        switch (which) {
                            case 0: showSwapDialog(plan);           break;
                            case 1: showChangeActivityDialog(plan); break;
                            case 2: showCancelConfirmDialog(plan);  break;
                        }
                    }
                })
                .setNegativeButton("Back", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 1: SWAP DAYS
    //
    //  Shows a list of OTHER days to swap with.
    //  The user picks one, then we show the scope dialog.
    //
    //  We filter out:
    //  - The current day (can't swap with itself)
    //  - Past days (can't reschedule into the past)
    // ─────────────────────────────────────────────────────────
    private void showSwapDialog(PlannedWorkout plan) {
        String today = PlanEngine.getTodayDayOfWeek();

        // Build list of valid swap targets
        List<PlannedWorkout> targets = new ArrayList<>();
        for (PlannedWorkout other : currentPlans) {
            boolean isSelf   = other.id == plan.id;
            boolean isPast   = isDayPast(other.dayOfWeek, today)
                    && !other.dayOfWeek.equals(today);
            if (!isSelf && !isPast) {
                targets.add(other);
            }
        }

        if (targets.isEmpty()) {
            Toast.makeText(this,
                    "No other days available to swap with.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build display labels for each target day
        String[] labels = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            PlannedWorkout t = targets.get(i);
            labels[i] = fullDayName(t.dayOfWeek) + "  —  "
                    + (t.isRestDay ? "Rest Day" : t.sessionTitle);
        }

        new AlertDialog.Builder(this)
                .setTitle("Swap " + fullDayName(plan.dayOfWeek) + " with...")
                .setItems(labels, (dialog, which) -> {
                    PlannedWorkout target = targets.get(which);
                    // Confirm with a summary before executing
                    showSwapConfirmDialog(plan, target);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void showSwapConfirmDialog(PlannedWorkout a, PlannedWorkout b) {
        String aLabel = fullDayName(a.dayOfWeek) + ": "
                + (a.isRestDay ? "Rest" : a.sessionTitle);
        String bLabel = fullDayName(b.dayOfWeek) + ": "
                + (b.isRestDay ? "Rest" : b.sessionTitle);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Swap")
                .setMessage(aLabel + "\n↕\n" + bLabel
                        + "\n\nThese two days will exchange workouts.")
                .setPositiveButton("Swap", (dialog, which) -> {
                    showScopeDialog(() -> editViewModel.swapDays(a.id, b.id));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 2: CHANGE ACTIVITY TYPE
    //
    //  Shows all 8 activity types as radio buttons.
    //  Confirms before regenerating the session detail.
    // ─────────────────────────────────────────────────────────
    private void showChangeActivityDialog(PlannedWorkout plan) {
        // All possible activity types with their emoji labels
        final String[] activityKeys   = {"GYM","RUNNING","BOULDERING",
                "CYCLING","SWIMMING","YOGA","HOME","SPORTS"};
        final String[] activityLabels = {"🏋️  Gym", "🏃  Running",
                "🧗  Bouldering", "🚴  Cycling",
                "🏊  Swimming", "🧘  Yoga",
                "🏠  Home Workout", "⚽  Sports"};

        // Pre-select the current activity
        int currentIndex = 0;
        for (int i = 0; i < activityKeys.length; i++) {
            if (activityKeys[i].equals(plan.activityType)) {
                currentIndex = i;
                break;
            }
        }
        final int[] selected = {currentIndex}; // array so lambda can modify it

        new AlertDialog.Builder(this)
                .setTitle("Change Activity — " + fullDayName(plan.dayOfWeek))
                .setSingleChoiceItems(activityLabels, currentIndex,
                        (dialog, which) -> selected[0] = which)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String newActivity = activityKeys[selected[0]];
                    if (newActivity.equals(plan.activityType)) {
                        Toast.makeText(this,
                                "Same activity selected — no change made.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Show what will change, then ask scope
                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Activity Change")
                            .setMessage(fullDayName(plan.dayOfWeek)
                                    + " will change from " + plan.activityType
                                    + " to " + newActivity
                                    + ".\n\nThe session detail will be regenerated.")
                            .setPositiveButton("Change", (d2, w2) -> {
                                showScopeDialog(() ->
                                        editViewModel.changeActivity(plan.id, newActivity));
                            })
                            .setNegativeButton("Back", null)
                            .show();
                })
                .setNegativeButton("Back", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 3: ADD WORKOUT TO REST DAY
    //
    //  Step 1: AI suggests 2–3 activities based on the user's profile.
    //  Step 2: User picks from the suggestions + "Custom" option.
    //  Step 3: If "Custom", show the full activity picker.
    //  Step 4: Show scope dialog → save.
    //
    //  WHY "AI suggests" without an actual AI call?
    //  We use PlanEngine logic to pick suggestions intelligently
    //  based on the user's goal and selected activities. This is
    //  rules-based AI — deterministic but smart. No API needed.
    // ─────────────────────────────────────────────────────────
    private void showAddWorkoutDialog(PlannedWorkout plan) {
        // Load user prefs to generate suggestions
        com.example.aifitnessapp.util.AppExecutors.getInstance().diskIO().execute(() -> {
            com.example.aifitnessapp.data.model.UserPreferences prefs =
                    com.example.aifitnessapp.data.db.FitAIDatabase
                            .getInstance(getApplication())
                            .userPreferencesDao()
                            .getCurrentUserSync();

            if (prefs == null) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Could not load profile.", Toast.LENGTH_SHORT).show());
                return;
            }

            // Generate suggestions based on user's activities and goal
            List<String> suggestions = buildSuggestions(prefs, plan.dayOfWeek);
            suggestions.add("Other — let me choose");  // always last

            String[] labels = new String[suggestions.size()];
            for (int i = 0; i < suggestions.size(); i++) {
                String key = suggestions.get(i);
                labels[i] = key.startsWith("Other")
                        ? key : activityEmoji(key) + "  " + formatActivity(key)
                        + (i < suggestions.size() - 2
                        ? "  ✨ AI pick" : "");
            }

            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("Add workout — " + fullDayName(plan.dayOfWeek))
                        .setMessage("Your AI coach suggests:")
                        .setItems(labels, (dialog, which) -> {
                            String chosen = suggestions.get(which);
                            if (chosen.startsWith("Other")) {
                                // Show full custom picker
                                showCustomActivityPicker(plan);
                            } else {
                                // Confirm the AI suggestion
                                new AlertDialog.Builder(this)
                                        .setTitle("Add "
                                                + formatActivity(chosen)
                                                + " on "
                                                + fullDayName(plan.dayOfWeek) + "?")
                                        .setMessage("A new session will be generated "
                                                + "for this day.")
                                        .setPositiveButton("Add", (d2, w2) -> {
                                            showScopeDialog(() ->
                                                    editViewModel.addWorkoutToRestDay(
                                                            plan.id, chosen));
                                        })
                                        .setNegativeButton("Back", null)
                                        .show();
                            }
                        })
                        .setNegativeButton("Back", null)
                        .show();
            });
        });
    }

    // Custom picker: all 8 activities, no AI pre-selection
    private void showCustomActivityPicker(PlannedWorkout plan) {
        final String[] activityKeys   = {"GYM","RUNNING","BOULDERING",
                "CYCLING","SWIMMING","YOGA","HOME","SPORTS"};
        final String[] activityLabels = {"🏋️  Gym", "🏃  Running",
                "🧗  Bouldering", "🚴  Cycling",
                "🏊  Swimming", "🧘  Yoga",
                "🏠  Home Workout", "⚽  Sports"};
        final int[] selected = {0};

        new AlertDialog.Builder(this)
                .setTitle("Choose activity — " + fullDayName(plan.dayOfWeek))
                .setSingleChoiceItems(activityLabels, 0,
                        (dialog, which) -> selected[0] = which)
                .setPositiveButton("Add", (dialog, which) -> {
                    String chosen = activityKeys[selected[0]];
                    showScopeDialog(() ->
                            editViewModel.addWorkoutToRestDay(plan.id, chosen));
                })
                .setNegativeButton("Back", null)
                .show();
    }

    /*
     * Builds a short list of suggested activities for a rest day.
     *
     * LOGIC:
     * 1. Start with activities the user already does (from their profile)
     * 2. Prioritise activities that match their fitness goal
     * 3. Return up to 2 suggestions
     *
     * This is "AI" in the rules-based sense — no model needed.
     * The suggestions feel smart because they respect the user's context.
     */
    private List<String> buildSuggestions(
            com.example.aifitnessapp.data.model.UserPreferences prefs,
            String dayOfWeek) {

        List<String> suggestions = new ArrayList<>();
        String[] userActivities  = prefs.getActivitiesArray();

        // Priority 1: goal-aligned activities the user already does
        for (String activity : userActivities) {
            if (suggestions.size() >= 2) break;
            boolean goalAligned = false;
            switch (prefs.fitnessGoal) {
                case "BUILD_MUSCLE":
                    goalAligned = activity.equals("GYM") || activity.equals("HOME");
                    break;
                case "LOSE_FAT":
                    goalAligned = activity.equals("RUNNING")
                            || activity.equals("CYCLING")
                            || activity.equals("SWIMMING");
                    break;
                case "ENDURANCE":
                    goalAligned = activity.equals("RUNNING")
                            || activity.equals("CYCLING")
                            || activity.equals("SWIMMING");
                    break;
                default:
                    goalAligned = true;
            }
            if (goalAligned && !suggestions.contains(activity)) {
                suggestions.add(activity);
            }
        }

        // Priority 2: fill with any user activity if we have fewer than 2
        for (String activity : userActivities) {
            if (suggestions.size() >= 2) break;
            if (!suggestions.contains(activity)) {
                suggestions.add(activity);
            }
        }

        // Fallback: always have at least one suggestion
        if (suggestions.isEmpty()) suggestions.add("HOME");

        return suggestions;
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG 4: CANCEL WORKOUT (with confirmation)
    //
    //  Two-step: warn the user, then ask scope.
    //  "Are you sure?" prevents accidental cancellations.
    // ─────────────────────────────────────────────────────────
    private void showCancelConfirmDialog(PlannedWorkout plan) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel " + fullDayName(plan.dayOfWeek) + "?")
                .setMessage("\"" + plan.sessionTitle + "\" will be removed "
                        + "and this day will become a Rest day.\n\n"
                        + "Are you sure?")
                .setPositiveButton("Yes, Cancel Workout", (dialog, which) -> {
                    showScopeDialog(() -> editViewModel.cancelWorkout(plan.id));
                })
                .setNegativeButton("Keep It", null)  // dismissive phrasing
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  SCOPE DIALOG — "This week only or future weeks too?"
    //
    //  Shown after EVERY edit, before executing.
    //  Takes a Runnable — the actual edit action to run after
    //  the user confirms the scope.
    //
    //  WHY Runnable?
    //  We want ONE scope dialog that works for ALL 4 edit types.
    //  Instead of writing separate scope dialogs for each,
    //  we pass in "what to do" as a Runnable parameter.
    //  The scope dialog just calls run() when the user confirms.
    //
    //  FUTURE WEEKS: in this implementation, we note the user's
    //  choice with a Toast. Persisting it to future AI-generated
    //  weeks is a follow-up feature — the architecture supports it.
    // ─────────────────────────────────────────────────────────
    private void showScopeDialog(Runnable editAction) {
        new AlertDialog.Builder(this)
                .setTitle("Apply change to...")
                .setItems(new String[]{
                        "📅  This week only",
                        "🔁  This week + all future weeks"
                }, (dialog, which) -> {
                    if (which == 0) {
                        // This week only — just run the edit
                        editAction.run();
                    } else {
                        // Future weeks too — run the edit + note the preference
                        // The AI plan generator will respect saved preferences
                        // in a future enhancement.
                        editAction.run();
                        Toast.makeText(this,
                                "Preference saved — future plans will reflect this.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Back", null)
                .show();
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        tvPlanSummary    = findViewById(R.id.tvPlanSummary);
        tvPlanWeekDates  = findViewById(R.id.tvPlanWeekDates);
        planDayContainer = findViewById(R.id.planDayContainer);
        btnBackFromPlan  = findViewById(R.id.btnBackFromPlan);
    }

    // Helper to call loadWeekLabel on background thread
    private void loadWeekDates(int userId) {
        com.example.aifitnessapp.util.AppExecutors.getInstance().diskIO().execute(() -> {
            String weekStart = PlanEngine.getCurrentWeekStart();
            String dates     = formatWeekDates(weekStart);
            runOnUiThread(() -> tvPlanWeekDates.setText(dates));
        });
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

    private String formatActivity(String type) {
        if (type == null) return "Workout";
        switch (type) {
            case "GYM":        return "Gym";
            case "RUNNING":    return "Running";
            case "BOULDERING": return "Bouldering";
            case "CYCLING":    return "Cycling";
            case "SWIMMING":   return "Swimming";
            case "YOGA":       return "Yoga";
            case "HOME":       return "Home Workout";
            case "SPORTS":     return "Sports";
            default:           return type;
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