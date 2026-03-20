package com.example.aifitnessapp.ui.home;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.util.WorkoutTimer;

public class WorkoutDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        // Unpack data passed via intent extras
        String date          = getIntent().getStringExtra("date");
        String sessionTitle  = getIntent().getStringExtra("sessionTitle");
        String activityType  = getIntent().getStringExtra("activityType");
        String status        = getIntent().getStringExtra("completionStatus");
        int    effort        = getIntent().getIntExtra("perceivedEffort", 0);
        String notes         = getIntent().getStringExtra("notes");
        String photoPath     = getIntent().getStringExtra("photoPath");
        int    durationSecs  = getIntent().getIntExtra("durationSeconds", 0);
        String coachNote     = getIntent().getStringExtra("coachNote");

        bindAndRender(date, sessionTitle, activityType, status,
                effort, notes, photoPath, durationSecs, coachNote);

        findViewById(R.id.btnBackFromDetail).setOnClickListener(v -> finish());
    }

    private void bindAndRender(String date, String sessionTitle,
                               String activityType, String status,
                               int effort, String notes,
                               String photoPath, int durationSecs,
                               String coachNote) {

        // Photo
        ImageView ivPhoto = findViewById(R.id.ivDetailPhoto);
        if (photoPath != null && !photoPath.isEmpty()) {
            ivPhoto.setVisibility(View.VISIBLE);
            ivPhoto.setImageURI(Uri.parse(photoPath));
        } else {
            ivPhoto.setVisibility(View.GONE);
        }

        // Header
        TextView tvEmoji = findViewById(R.id.tvDetailEmoji);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDate  = findViewById(R.id.tvDetailDate);
        tvEmoji.setText(activityEmoji(activityType));
        tvTitle.setText(sessionTitle != null ? sessionTitle : "Workout");
        tvDate.setText(date != null ? date : "");

        // Status + effort
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvEffort = findViewById(R.id.tvDetailEffort);
        tvStatus.setText(statusLabel(status));
        tvStatus.setTextColor(statusColor(status));

        if (effort > 0) {
            tvEffort.setText(effortDots(effort)
                    + "  " + effortLabel(effort));
            tvEffort.setVisibility(View.VISIBLE);
        } else {
            tvEffort.setVisibility(View.GONE);
        }

        // Duration
        TextView tvDuration = findViewById(R.id.tvDetailDuration);
        if (durationSecs > 0) {
            tvDuration.setText("⏱  " + WorkoutTimer.format(durationSecs));
            tvDuration.setVisibility(View.VISIBLE);
        } else {
            tvDuration.setVisibility(View.GONE);
        }

        // Coach note
        TextView tvCoachNote = findViewById(R.id.tvDetailCoachNote);
        if (coachNote != null && !coachNote.isEmpty()) {
            tvCoachNote.setText(coachNote);
            tvCoachNote.setVisibility(View.VISIBLE);
        } else {
            tvCoachNote.setVisibility(View.GONE);
        }

        // Exercises — parsed from notes
        LinearLayout exerciseContainer = findViewById(R.id.detailExerciseContainer);
        TextView tvNotesRaw = findViewById(R.id.tvDetailNotesRaw);

        if (notes != null && !notes.isEmpty()) {
            // Split notes into lines — each line is one exercise or note
            String[] lines = notes.split("\n");
            boolean hasExercises = false;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Exercise lines start with ✅ or ⏭️
                if (line.startsWith("✅") || line.startsWith("⏭️")) {
                    hasExercises = true;
                    TextView tv = new TextView(this);
                    tv.setText(line);
                    tv.setTextSize(14f);
                    tv.setTextColor(line.startsWith("✅")
                            ? 0xFF2E7D32 : 0xFF9E9E9E);
                    tv.setPadding(0, 4, 0, 4);
                    exerciseContainer.addView(tv);
                }
            }

            // Show raw user notes (lines after blank line)
            StringBuilder userNotes = new StringBuilder();
            boolean afterBlank = false;
            for (String line : lines) {
                if (line.trim().isEmpty()) { afterBlank = true; continue; }
                if (afterBlank) userNotes.append(line).append("\n");
            }

            if (userNotes.length() > 0) {
                tvNotesRaw.setText(userNotes.toString().trim());
                tvNotesRaw.setVisibility(View.VISIBLE);
            } else {
                tvNotesRaw.setVisibility(View.GONE);
            }

            if (!hasExercises) {
                exerciseContainer.setVisibility(View.GONE);
            }
        } else {
            exerciseContainer.setVisibility(View.GONE);
            tvNotesRaw.setVisibility(View.GONE);
        }
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

    private String statusLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "COMPLETED": return "✅  Completed";
            case "MODIFIED":  return "✏️  Modified";
            case "SKIPPED":   return "⏭️  Skipped";
            default:          return s;
        }
    }

    private int statusColor(String s) {
        if (s == null) return 0xFF9E9E9E;
        switch (s) {
            case "COMPLETED": return 0xFF2E7D32;
            case "MODIFIED":  return 0xFFE65100;
            default:          return 0xFF9E9E9E;
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
}