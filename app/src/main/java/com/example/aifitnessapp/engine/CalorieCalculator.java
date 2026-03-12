package com.example.aifitnessapp.engine;

public class CalorieCalculator {

    // STEP 1: Basal Metabolic Rate — calories burned doing absolutely nothing
    public static float calculateBMR(float weightKg, float heightCm, int age, String gender) {
        if (gender.equalsIgnoreCase("male")) {
            return (10 * weightKg) + (6.25f * heightCm) - (5 * age) + 5;
        } else {
            return (10 * weightKg) + (6.25f * heightCm) - (5 * age) - 161;
        }
    }

    // STEP 2: Total Daily Energy Expenditure — real calories burned with activity
    public static float calculateTDEE(float bmr, String activityLevel) {
        switch (activityLevel) {
            case "sedentary":   return bmr * 1.2f;
            case "light":       return bmr * 1.375f;
            case "moderate":    return bmr * 1.55f;
            case "active":      return bmr * 1.725f;
            case "very_active": return bmr * 1.9f;
            default:            return bmr * 1.2f;
        }
    }

    // STEP 3: Adjust TDEE based on goal
    public static int calculateCalorieTarget(float tdee, String goal) {
        switch (goal) {
            case "fat_loss":    return Math.round(tdee - 500); // deficit
            case "muscle_gain": return Math.round(tdee + 300); // surplus
            case "endurance":   return Math.round(tdee);       // maintenance
            default:            return Math.round(tdee);
        }
    }

    // STEP 4: Recommended workout days per week
    public static int recommendWorkoutFrequency(String goal, String activityLevel) {
        if (goal.equals("muscle_gain"))  return 5;
        if (goal.equals("fat_loss"))     return 4;
        return 3; // endurance
    }

    // STEP 5: Macro split in grams [protein, carbs, fat]
    public static int[] calculateMacros(int calories, String goal) {
        float proteinPct, carbsPct, fatPct;

        switch (goal) {
            case "muscle_gain":
                proteinPct = 0.35f; carbsPct = 0.45f; fatPct = 0.20f; break;
            case "fat_loss":
                proteinPct = 0.40f; carbsPct = 0.35f; fatPct = 0.25f; break;
            default: // endurance
                proteinPct = 0.25f; carbsPct = 0.55f; fatPct = 0.20f; break;
        }

        // 1g protein = 4 cal, 1g carbs = 4 cal, 1g fat = 9 cal
        int proteinG = Math.round((calories * proteinPct) / 4);
        int carbsG   = Math.round((calories * carbsPct)  / 4);
        int fatG     = Math.round((calories * fatPct)    / 9);

        return new int[]{proteinG, carbsG, fatG};
    }
}