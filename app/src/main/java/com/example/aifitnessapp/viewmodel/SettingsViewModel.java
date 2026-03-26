package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.repository.AuthRepository;
import com.example.aifitnessapp.repository.SettingsRepository;
import com.example.aifitnessapp.util.AppExecutors;
import com.example.aifitnessapp.util.SessionManager;

/*
 * SettingsViewModel
 *
 * Owns the state for SettingsActivity:
 *  - currentPrefs   → the loaded UserPreferences, used to populate UI
 *  - saveResult     → did the last save succeed?
 *  - errorMessage   → what went wrong?
 *  - isLoading      → show spinner during background operations
 *  - clearResult    → did clearAllData succeed? (navigates to onboarding)
 *
 * WHY a separate clearResult LiveData?
 * clearAllData is the only operation that changes screen destination
 * (navigates to OnboardingActivity instead of staying in Settings).
 * Keeping it separate makes the observer logic in the Activity cleaner —
 * no need to check "which operation just finished?" in a single observer.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository settingsRepo;
    private final AuthRepository     authRepo;
    private final SessionManager     session;

    // ── Observable state ──────────────────────────────────────
    public final MutableLiveData<UserPreferences> currentPrefs =
            new MutableLiveData<>(null);

    public final MutableLiveData<Boolean> saveResult   =
            new MutableLiveData<>(null);

    public final MutableLiveData<String>  errorMessage =
            new MutableLiveData<>(null);

    public final MutableLiveData<Boolean> isLoading    =
            new MutableLiveData<>(false);

    // Fires true when clearAllData completes — Activity navigates to onboarding
    public final MutableLiveData<Boolean> clearResult  =
            new MutableLiveData<>(null);

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        settingsRepo = new SettingsRepository(application);
        authRepo     = new AuthRepository(application);
        session      = new SessionManager(application);
    }

    // ─────────────────────────────────────────────────────────
    //  LOAD SETTINGS
    //
    //  Called once when SettingsActivity starts.
    //  Loads UserPreferences + session email into currentPrefs LiveData.
    //  The Activity observes currentPrefs to populate the UI rows.
    // ─────────────────────────────────────────────────────────
    public void loadSettings() {
        isLoading.setValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserPreferences prefs = settingsRepo.loadPreferences();
            currentPrefs.postValue(prefs);
            isLoading.postValue(false);
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE NAME
    // ─────────────────────────────────────────────────────────
    public void updateName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            errorMessage.setValue("Name cannot be empty.");
            return;
        }
        isLoading.setValue(true);
        settingsRepo.updateName(newName, new SettingsRepository.SettingsCallback() {
            @Override public void onSuccess() {
                // Reload prefs so the UI row reflects the new name
                reloadPrefs();
                saveResult.postValue(true);
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE WEIGHT
    // ─────────────────────────────────────────────────────────
    public void updateWeight(float newWeight) {
        if (newWeight < 20f || newWeight > 300f) {
            errorMessage.setValue("Please enter a valid weight (20–300 kg).");
            return;
        }
        isLoading.setValue(true);
        settingsRepo.updateWeight(newWeight, new SettingsRepository.SettingsCallback() {
            @Override public void onSuccess() {
                reloadPrefs();
                saveResult.postValue(true);
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE FITNESS GOAL
    //
    //  applyNow: true  = regenerate this week's plan immediately
    //            false = save preference, next week picks it up
    // ─────────────────────────────────────────────────────────
    public void updateFitnessGoal(String newGoal, boolean applyNow) {
        isLoading.setValue(true);
        settingsRepo.updateFitnessGoal(newGoal, applyNow,
                new SettingsRepository.SettingsCallback() {
                    @Override public void onSuccess() {
                        reloadPrefs();
                        saveResult.postValue(true);
                        isLoading.postValue(false);
                    }
                    @Override public void onError(String message) {
                        errorMessage.postValue(message);
                        isLoading.postValue(false);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE ACTIVITIES
    // ─────────────────────────────────────────────────────────
    public void updateActivities(String newActivities, boolean applyNow) {
        if (newActivities == null || newActivities.isEmpty()) {
            errorMessage.setValue("Please select at least one activity.");
            return;
        }
        isLoading.setValue(true);
        settingsRepo.updateActivities(newActivities, applyNow,
                new SettingsRepository.SettingsCallback() {
                    @Override public void onSuccess() {
                        reloadPrefs();
                        saveResult.postValue(true);
                        isLoading.postValue(false);
                    }
                    @Override public void onError(String message) {
                        errorMessage.postValue(message);
                        isLoading.postValue(false);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────
    //  RESET PLAN
    // ─────────────────────────────────────────────────────────
    public void resetPlan() {
        isLoading.setValue(true);
        settingsRepo.resetPlan(new SettingsRepository.SettingsCallback() {
            @Override public void onSuccess() {
                saveResult.postValue(true);
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  CLEAR ALL DATA
    // ─────────────────────────────────────────────────────────
    public void clearAllData() {
        isLoading.setValue(true);
        settingsRepo.clearAllData(new SettingsRepository.SettingsCallback() {
            @Override public void onSuccess() {
                isLoading.postValue(false);
                clearResult.postValue(true);
            }
            @Override public void onError(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  LOG OUT
    //
    //  Synchronous — just clears SharedPreferences.
    //  No DB operation needed. The Activity navigates immediately.
    // ─────────────────────────────────────────────────────────
    public void logout() {
        authRepo.logout();
    }

    // ── Helpers ───────────────────────────────────────────────

    /*
     * Reloads UserPreferences from Room after a save.
     * This ensures currentPrefs LiveData always reflects
     * the latest saved state, not the state before the edit.
     * Runs on background thread — posts result via postValue().
     */
    private void reloadPrefs() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserPreferences prefs = settingsRepo.loadPreferences();
            currentPrefs.postValue(prefs);
        });
    }

    // Returns the logged-in email from SharedPreferences
    public String getEmail() {
        return session.getUserEmail();
    }

    // Reset state so observers don't re-fire after navigation
    public void resetSaveResult() {
        saveResult.setValue(null);
        errorMessage.setValue(null);
    }
}