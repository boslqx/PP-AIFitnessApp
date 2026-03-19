package com.example.aifitnessapp.engine;

import com.example.aifitnessapp.data.model.ExerciseEntry;
import java.util.ArrayList;
import java.util.List;

/*
 * Parses PlannedWorkout.sessionDetail into a list of ExerciseEntry objects.
 *
 * Expected format (from PlanEngine):
 *   "4×5 Squat · 4×5 Bench press · 3×8 Barbell row"
 *   "Circuit ×3: DB thrusters ×15 · Renegade rows ×10"
 *
 * PARSING RULES:
 *   - Split on " · " to get individual items
 *   - First token before first space that contains × = sets/reps
 *   - Rest of the string = exercise name
 *   - If no × found, treat whole string as name with "—" as sets
 */
public class ExerciseParser {

    public static List<ExerciseEntry> parse(String sessionDetail) {
        List<ExerciseEntry> entries = new ArrayList<>();
        if (sessionDetail == null || sessionDetail.isEmpty()) return entries;

        // Handle circuit header e.g. "Circuit ×3: ..."
        String detail = sessionDetail;
        String prefix = "";
        if (detail.contains(":")) {
            int colonIdx = detail.indexOf(":");
            prefix = detail.substring(0, colonIdx).trim() + " — ";
            detail = detail.substring(colonIdx + 1).trim();
        }

        // Split on · separator
        String[] parts = detail.split(" · ");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // Try to split sets/reps from name
            // Pattern: starts with digits or ×, e.g. "4×5", "×3", "3×"
            String setsReps = "";
            String name     = part;

            String[] tokens = part.split(" ", 2);
            if (tokens.length >= 2 && (tokens[0].contains("×")
                    || tokens[0].matches("\\d+.*"))) {
                setsReps = tokens[0];
                name     = prefix + tokens[1];
            } else {
                name = prefix + part;
            }

            entries.add(new ExerciseEntry(name, setsReps));
        }

        return entries;
    }

    /*
     * Serializes exercise list back to a readable string for storage in notes.
     * Format: "✅ 4×5 Squat | ✅ 3×5 Bench press | ⏭️ 3×8 Row (planned: 3×8)"
     */
    public static String serialize(List<ExerciseEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            ExerciseEntry e = entries.get(i);
            sb.append(e.completed ? "✅ " : "⏭️ ");
            if (!e.actual.isEmpty()) sb.append(e.actual).append(" ");
            sb.append(e.name);
            if (!e.actual.equals(e.original) && !e.original.isEmpty()) {
                sb.append(" (planned: ").append(e.original).append(")");
            }
            if (i < entries.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}