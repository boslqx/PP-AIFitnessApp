package com.example.aifitnessapp.ui.habit;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.HabitViewModel;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class HabitTrackerActivity extends AppCompatActivity {

    private HabitViewModel viewModel;
    private LinearLayout habitListContainer;
    private ProgressBar pbHabits;
    private TextView tvCompletionSummary;
    private TextInputEditText etNewHabit;
    private RadioGroup rgCategory;
    private int currentUserId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_tracker);

        viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        bindViews();

        ((TextView) findViewById(R.id.tvHabitDate))
                .setText(viewModel.getTodayDisplay());

        // Get userId, load habits
        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            currentUserId = user.id;
            viewModel.loadTodayHabits(user.id);

            // Observe today's habit list — rebuilds UI on every change
            viewModel.todayHabits.observe(this, habits -> {
                buildHabitList(habits);
                viewModel.refreshSummary(habits);
                updateProgressBar(habits);
            });
        });

        // Observe summary text
        viewModel.completionSummary.observe(this, summary ->
                tvCompletionSummary.setText(summary));

        // Add habit button
        findViewById(R.id.btnAddHabit).setOnClickListener(v -> addHabit());

        // Back button
        findViewById(R.id.btnBackFromHabits).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void bindViews() {
        habitListContainer  = findViewById(R.id.habitListContainer);
        pbHabits            = findViewById(R.id.pbHabits);
        tvCompletionSummary = findViewById(R.id.tvCompletionSummary);
        etNewHabit          = findViewById(R.id.etNewHabit);
        rgCategory          = findViewById(R.id.rgCategory);
    }

    /*
     * Inflates item_habit.xml for each HabitLog and wires up:
     *   - tap the check circle → toggles completed
     *   - tap ✕ → deletes the habit
     *
     * Completed habits get:
     *   - green filled circle "●" instead of "○"
     *   - strikethrough on the habit name
     *   - reduced alpha to visually de-emphasise
     */
    private void buildHabitList(List<HabitLog> habits) {
        habitListContainer.removeAllViews();

        if (habits == null || habits.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No habits for today yet.\nAdd your first habit below! 👇");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(14f);
            empty.setPadding(0, 32, 0, 0);
            habitListContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (HabitLog habit : habits) {
            View item = inflater.inflate(R.layout.item_habit,
                    habitListContainer, false);

            TextView tvCheck    = item.findViewById(R.id.tvHabitCheck);
            TextView tvName     = item.findViewById(R.id.tvHabitName);
            TextView tvCategory = item.findViewById(R.id.tvHabitCategory);
            TextView tvDelete   = item.findViewById(R.id.tvHabitDelete);

            tvName.setText(habit.habitName);
            tvCategory.setText(categoryEmoji(habit.category) + " " + habit.category);

            // Visual state for completed vs incomplete
            applyCompletedStyle(tvCheck, tvName, habit.completed);

            // Tap circle → toggle
            tvCheck.setOnClickListener(v -> {
                viewModel.toggleHabit(habit);
                // No need to call buildHabitList() — LiveData re-delivers
                // automatically after the DB update
            });

            // Long-press name → also toggle (more tap area)
            tvName.setOnClickListener(v -> viewModel.toggleHabit(habit));

            // Tap ✕ → delete
            tvDelete.setOnClickListener(v -> viewModel.deleteHabit(habit));

            habitListContainer.addView(item);
        }
    }

    private void applyCompletedStyle(TextView tvCheck, TextView tvName, boolean completed) {
        if (completed) {
            tvCheck.setText("●");
            tvCheck.setTextColor(0xFF4CAF50); // green
            tvName.setAlpha(0.5f);
            // Strikethrough the habit name
            tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvCheck.setText("○");
            tvCheck.setTextColor(0xFF9E9E9E); // grey
            tvName.setAlpha(1f);
            tvName.setPaintFlags(tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void addHabit() {
        String name = etNewHabit.getText().toString().trim();
        if (name.isEmpty()) {
            etNewHabit.setError("Please enter a habit name");
            return;
        }

        // Get selected category from RadioGroup
        int selectedId = rgCategory.getCheckedRadioButtonId();
        String category = "exercise"; // default
        if (selectedId != -1) {
            category = findViewById(selectedId) instanceof android.widget.RadioButton
                    ? ((android.widget.RadioButton) findViewById(selectedId)).getTag().toString()
                    : "exercise";
        }

        viewModel.addHabit(currentUserId, name, category);

        // Clear input fields
        etNewHabit.setText("");
        rgCategory.clearCheck();

        Toast.makeText(this, "Habit added! ✅", Toast.LENGTH_SHORT).show();
    }

    /*
     * Updates the compact progress bar in the header.
     * progress = (completed / total) * 100
     */
    private void updateProgressBar(List<HabitLog> habits) {
        if (habits == null || habits.isEmpty()) {
            pbHabits.setProgress(0);
            return;
        }
        int done = 0;
        for (HabitLog h : habits) if (h.completed) done++;
        pbHabits.setProgress((int) ((done / (float) habits.size()) * 100));
    }

    private String categoryEmoji(String category) {
        if (category == null) return "📌";
        switch (category) {
            case "exercise":  return "💪";
            case "nutrition": return "🥗";
            case "sleep":     return "😴";
            case "mindset":   return "🧠";
            default:          return "📌";
        }
    }
}