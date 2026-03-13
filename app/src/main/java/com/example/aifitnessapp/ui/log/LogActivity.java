package com.example.aifitnessapp.ui.log;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.viewmodel.LogViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class LogActivity extends AppCompatActivity {

    private LogViewModel viewModel;
    private int currentUserId = 1;

    // Workout
    private RadioGroup rgWorkoutType, rgIntensity;
    private TextInputEditText etDuration;

    // Nutrition
    private TextInputEditText etCalories, etProtein, etCarbs, etFat;

    // Sleep
    private TextInputEditText etSleepHours;
    private RadioGroup rgSleepQuality;

    // Water
    private TextInputEditText etWater;

    // Mood
    private RadioGroup rgMood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        viewModel = new ViewModelProvider(this).get(LogViewModel.class);

        bindViews();

        // Set today's date in header
        ((TextView) findViewById(R.id.tvLogDate)).setText(viewModel.getTodayDate());

        // Get real userId from DB
        viewModel.currentUser.observe(this, user -> {
            if (user != null) currentUserId = user.id;
        });

        // Observe save → navigate to summary
        viewModel.saveSuccess.observe(this, success -> {
            if (success) {
                Intent intent = new Intent(this, LogSummaryActivity.class);
                // Pass all logged values to the summary screen
                intent.putExtra("calories",   getInt(etCalories));
                intent.putExtra("protein",    getFloat(etProtein));
                intent.putExtra("carbs",      getFloat(etCarbs));
                intent.putExtra("fat",        getFloat(etFat));
                intent.putExtra("sleep",      getInt(etSleepHours));
                intent.putExtra("water",      getInt(etWater));
                intent.putExtra("mood",       getSelectedTag(rgMood));
                intent.putExtra("workout",    getSelectedTag(rgWorkoutType));
                intent.putExtra("duration",   getInt(etDuration));
                intent.putExtra("intensity",  getIntTag(rgIntensity));
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.btnSaveLog).setOnClickListener(v -> {
            if (!validate()) return;

            viewModel.saveLog(
                    currentUserId,
                    getInt(etCalories),
                    getFloat(etProtein), getFloat(etCarbs), getFloat(etFat),
                    getInt(etSleepHours), getIntTag(rgSleepQuality),
                    getInt(etWater),
                    getSelectedTag(rgMood),
                    getSelectedTag(rgWorkoutType),
                    getInt(etDuration),
                    getIntTag(rgIntensity)
            );
        });
    }

    private void bindViews() {
        rgWorkoutType  = findViewById(R.id.rgWorkoutType);
        rgIntensity    = findViewById(R.id.rgIntensity);
        etDuration     = findViewById(R.id.etDuration);
        etCalories     = findViewById(R.id.etCalories);
        etProtein      = findViewById(R.id.etProtein);
        etCarbs        = findViewById(R.id.etCarbs);
        etFat          = findViewById(R.id.etFat);
        etSleepHours   = findViewById(R.id.etSleepHours);
        rgSleepQuality = findViewById(R.id.rgSleepQuality);
        etWater        = findViewById(R.id.etWater);
        rgMood         = findViewById(R.id.rgMood);
    }

    private boolean validate() {
        // Workout type required
        if (rgWorkoutType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a workout type (or Rest)", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Calories required
        if (etCalories.getText().toString().trim().isEmpty()) {
            etCalories.setError("Required");
            return false;
        }
        // Sleep hours required
        if (etSleepHours.getText().toString().trim().isEmpty()) {
            etSleepHours.setError("Required");
            return false;
        }
        // Mood required
        if (rgMood.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your mood", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────

    private int getInt(TextInputEditText field) {
        String s = field.getText().toString().trim();
        return s.isEmpty() ? 0 : Integer.parseInt(s);
    }

    private float getFloat(TextInputEditText field) {
        String s = field.getText().toString().trim();
        return s.isEmpty() ? 0f : Float.parseFloat(s);
    }

    // Gets the android:tag string from the checked RadioButton
    private String getSelectedTag(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return "none";
        RadioButton rb = findViewById(id);
        return rb.getTag().toString();
    }

    // Gets the android:tag as an integer
    private int getIntTag(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return 0;
        RadioButton rb = findViewById(id);
        return Integer.parseInt(rb.getTag().toString());
    }
}