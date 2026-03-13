package com.example.aifitnessapp.ui.log;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.engine.CalorieCalculator;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.MainViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.aifitnessapp.repository.ConsistencyScoreRepository;

public class LogSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_summary);

        Bundle data = getIntent().getExtras();
        if (data == null) return;

        // Extract all logged values passed from LogActivity
        int    calories  = data.getInt("calories");
        float  protein   = data.getFloat("protein");
        float  carbs     = data.getFloat("carbs");
        float  fat       = data.getFloat("fat");
        int    sleep     = data.getInt("sleep");
        int    water     = data.getInt("water");
        String mood      = data.getString("mood", "okay");
        String workout   = data.getString("workout", "rest");
        int    duration  = data.getInt("duration");
        int    intensity = data.getInt("intensity");

        // Bind views
        TextView tvDate         = findViewById(R.id.tvSummaryDate);
        TextView tvWorkoutType  = findViewById(R.id.tvSummaryWorkoutType);
        TextView tvDuration     = findViewById(R.id.tvSummaryWorkoutDuration);
        TextView tvCalBurned    = findViewById(R.id.tvSummaryCaloriesBurned);
        TextView tvCalories     = findViewById(R.id.tvSummaryCalories);
        TextView tvVsTarget     = findViewById(R.id.tvSummaryCalorieVsTarget);
        TextView tvProtein      = findViewById(R.id.tvSummaryProtein);
        TextView tvCarbs        = findViewById(R.id.tvSummaryCarbs);
        TextView tvFat          = findViewById(R.id.tvSummaryFat);
        TextView tvSleep        = findViewById(R.id.tvSummarySleep);
        TextView tvWater        = findViewById(R.id.tvSummaryWater);
        TextView tvMoodEmoji    = findViewById(R.id.tvSummaryMoodEmoji);
        TextView tvMood         = findViewById(R.id.tvSummaryMood);
        TextView tvAiFeedback   = findViewById(R.id.tvSummaryAiFeedback);

        // Populate date
        String today = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(today);

        // Populate workout
        tvWorkoutType.setText(formatWorkout(workout));
        tvDuration.setText(duration > 0 ? duration + " min" : "—");

        if (!workout.equals("rest") && duration > 0 && intensity > 0) {
            int burned = estimateBurned(workout, duration, intensity);
            tvCalBurned.setText("~" + burned + " kcal burned");
        } else {
            tvCalBurned.setText("Rest day 😴");
        }

        // Populate nutrition
        tvCalories.setText(calories + " kcal");
        tvProtein.setText("Protein\n" + (int) protein + "g");
        tvCarbs.setText("Carbs\n"     + (int) carbs   + "g");
        tvFat.setText("Fat\n"         + (int) fat      + "g");

        // Populate sleep, water, mood
        tvSleep.setText(sleep + "h");
        tvWater.setText(water >= 1000 ? (water / 1000f) + "L" : water + "ml");
        tvMoodEmoji.setText(moodEmoji(mood));
        tvMood.setText(capitalize(mood));

        // Get calorie target from DB to show vs-target comparison
        MainViewModel mainVM = new ViewModelProvider(this).get(MainViewModel.class);
        mainVM.currentUser.observe(this, user -> {
            if (user == null) return;
            int target = user.dailyCalorieTarget;
            tvVsTarget.setText(vsTargetLabel(calories, target));

            // Generate AI feedback based on all data
            tvAiFeedback.setText(generateFeedback(
                    calories, target, sleep, workout, mood, water));

            ConsistencyScoreRepository scoreRepo =
                    new ConsistencyScoreRepository(getApplication());
            scoreRepo.computeAndSaveTodayScore(user.id);
        });

        // Navigate back to dashboard
        Button btnBack = findViewById(R.id.btnBackToDashboard);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    // ── AI Feedback generator ────────────────────────────────

    private String generateFeedback(int calories, int target,
                                    int sleep, String workout, String mood, int water) {

        StringBuilder fb = new StringBuilder();

        // Calorie feedback
        int diff = calories - target;
        if (diff > 300)       fb.append("⚠️ You ate ").append(diff).append(" kcal over target. ");
        else if (diff < -400) fb.append("⚠️ You ate ").append(Math.abs(diff)).append(" kcal under target. ");
        else                  fb.append("✅ Calories are on track! ");

        // Sleep feedback
        if      (sleep >= 8) fb.append("Excellent sleep. ");
        else if (sleep >= 7) fb.append("Good sleep. ");
        else if (sleep >= 6) fb.append("Try to get 7–8h sleep tonight. ");
        else                 fb.append("⚠️ Less than 6h sleep affects recovery. Prioritise rest. ");

        // Water feedback
        if      (water >= 2500) fb.append("Great hydration! ");
        else if (water >= 2000) fb.append("Good water intake. ");
        else                    fb.append("💧 Try to drink at least 2L of water daily. ");

        // Mood feedback
        if (mood.equals("stressed")) fb.append("High stress detected — consider a light walk or stretching tomorrow.");
        else if (mood.equals("tired") && sleep < 7) fb.append("You're tired and under-slept — prioritise recovery tonight.");
        else if (mood.equals("great")) fb.append("Keep this energy! 🔥");

        return fb.toString().trim();
    }

    // ── Helpers ──────────────────────────────────────────────

    private String vsTargetLabel(int calories, int target) {
        if (target == 0) return "";
        int diff = calories - target;
        if (diff > 300)       return "⚠️ " + diff + " kcal over target";
        if (diff < -400)      return "⚠️ " + Math.abs(diff) + " kcal under target";
        return "✅ On target";
    }

    private String formatWorkout(String type) {
        switch (type) {
            case "strength":    return "💪 Strength";
            case "cardio":      return "🏃 Cardio";
            case "flexibility": return "🧘 Flexibility";
            case "rest":        return "😴 Rest Day";
            default:            return type;
        }
    }

    private String moodEmoji(String mood) {
        switch (mood) {
            case "great":   return "🔥";
            case "okay":    return "👍";
            case "tired":   return "😴";
            case "stressed":return "😤";
            default:        return "😐";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private int estimateBurned(String type, int minutes, int intensity) {
        float base;
        switch (type) {
            case "strength":    base = 6f; break;
            case "cardio":      base = 9f; break;
            case "flexibility": base = 3f; break;
            default:            base = 5f;
        }
        float factor = 0.55f + (intensity * 0.15f);
        return Math.round(base * minutes * factor);
    }
}