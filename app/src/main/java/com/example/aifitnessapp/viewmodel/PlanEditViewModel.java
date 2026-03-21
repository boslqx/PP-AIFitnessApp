package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.repository.PlanEditRepository;

/*
 * PlanEditViewModel — bridges PlanActivity and PlanEditRepository.
 *
 * WHAT LIVES HERE:
 *  - MutableLiveData fields that the Activity observes
 *  - One method per edit operation (swap, changeActivity, addWorkout, cancel)
 *  - Loading state so the UI can show a spinner during saves
 *
 * WHAT DOES NOT LIVE HERE:
 *  - Any Android View or Context reference (would cause memory leaks)
 *  - Direct database calls (that's the Repository's job)
 *  - Dialog logic (that's the Activity's job)
 *
 * LIVEDATA NAMING CONVENTION USED HERE:
 *  - editResult   → "did the last edit succeed or fail?"
 *  - editError    → "what went wrong?" (null if no error)
 *  - isLoading    → "should we show a spinner?"
 *
 * The Activity observes all three and reacts accordingly.
 */
public class PlanEditViewModel extends AndroidViewModel {

    private final PlanEditRepository editRepo;
    private final FitAIDatabase db;

    // ── Observable state ──────────────────────────────────────

    /*
     * editResult: fires true when any edit completes successfully.
     * The Activity observes this to dismiss dialogs and show a success toast.
     * We use Boolean (object) not boolean (primitive) so LiveData can hold null
     * as the "no result yet" initial state.
     */
    public final MutableLiveData<Boolean> editResult  = new MutableLiveData<>(null);

    /*
     * editError: holds the error message if something went wrong.
     * The Activity observes this to show an error toast.
     * Null means no error.
     */
    public final MutableLiveData<String>  editError   = new MutableLiveData<>(null);

    /*
     * isLoading: true while a background operation is running.
     * The Activity uses this to disable the edit buttons and show a spinner,
     * preventing the user from tapping again mid-save.
     */
    public final MutableLiveData<Boolean> isLoading   = new MutableLiveData<>(false);

    public PlanEditViewModel(@NonNull Application application) {
        super(application);
        editRepo = new PlanEditRepository(application);
        db       = FitAIDatabase.getInstance(application);
    }

    // ─────────────────────────────────────────────────────────
    //  Helper: load current user on background thread.
    //  Many edit operations need the UserPreferences (for equipment,
    //  fitness goal etc.) to regenerate session details correctly.
    // ─────────────────────────────────────────────────────────
    private UserPreferences getUserSync() {
        return db.userPreferencesDao().getCurrentUserSync();
    }

    // ─────────────────────────────────────────────────────────
    //  EDIT 1: SWAP TWO DAYS
    //
    //  Called by Activity after user confirms the swap.
    //  planIdA = the day the user tapped "Edit" on
    //  planIdB = the day the user chose to swap WITH
    // ─────────────────────────────────────────────────────────
    public void swapDays(int planIdA, int planIdB) {
        isLoading.setValue(true);   // show spinner immediately (main thread)
        resetResult();

        editRepo.swapDays(planIdA, planIdB, new PlanEditRepository.EditCallback() {
            @Override
            public void onSuccess() {
                // postValue() is used from background threads.
                // setValue() can only be used from the main thread.
                isLoading.postValue(false);
                editResult.postValue(true);
            }
            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                editError.postValue(message);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  EDIT 2: CHANGE ACTIVITY TYPE
    //
    //  planId          = the workout row to change
    //  newActivityType = e.g. "RUNNING", "GYM", "YOGA"
    // ─────────────────────────────────────────────────────────
    public void changeActivity(int planId, String newActivityType) {
        isLoading.setValue(true);
        resetResult();

        // We need UserPreferences on the background thread inside the repo,
        // so we fetch it there. But we also need it here to pass in.
        // The repo fetches it internally — see PlanEditRepository.changeActivity().
        com.example.aifitnessapp.util.AppExecutors.getInstance().diskIO().execute(() -> {
            UserPreferences prefs = getUserSync();
            if (prefs == null) {
                isLoading.postValue(false);
                editError.postValue("User profile not found.");
                return;
            }
            editRepo.changeActivity(planId, newActivityType, prefs,
                    new PlanEditRepository.EditCallback() {
                        @Override public void onSuccess() {
                            isLoading.postValue(false);
                            editResult.postValue(true);
                        }
                        @Override public void onError(String message) {
                            isLoading.postValue(false);
                            editError.postValue(message);
                        }
                    });
        });
    }

    // ─────────────────────────────────────────────────────────
    //  EDIT 3: ADD WORKOUT TO REST DAY
    //
    //  planId       = the rest day row to convert
    //  activityType = user's chosen activity
    // ─────────────────────────────────────────────────────────
    public void addWorkoutToRestDay(int planId, String activityType) {
        isLoading.setValue(true);
        resetResult();

        com.example.aifitnessapp.util.AppExecutors.getInstance().diskIO().execute(() -> {
            UserPreferences prefs = getUserSync();
            if (prefs == null) {
                isLoading.postValue(false);
                editError.postValue("User profile not found.");
                return;
            }
            editRepo.addWorkoutToRestDay(planId, activityType, prefs,
                    new PlanEditRepository.EditCallback() {
                        @Override public void onSuccess() {
                            isLoading.postValue(false);
                            editResult.postValue(true);
                        }
                        @Override public void onError(String message) {
                            isLoading.postValue(false);
                            editError.postValue(message);
                        }
                    });
        });
    }

    // ─────────────────────────────────────────────────────────
    //  EDIT 4: CANCEL A WORKOUT DAY
    //
    //  planId = the workout to cancel (becomes a Rest day)
    // ─────────────────────────────────────────────────────────
    public void cancelWorkout(int planId) {
        isLoading.setValue(true);
        resetResult();

        editRepo.cancelWorkout(planId, new PlanEditRepository.EditCallback() {
            @Override public void onSuccess() {
                isLoading.postValue(false);
                editResult.postValue(true);
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                editError.postValue(message);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  HELPER: reset result/error before each new operation
    //  so old values don't re-trigger the observer accidentally.
    // ─────────────────────────────────────────────────────────
    private void resetResult() {
        editResult.setValue(null);
        editError.setValue(null);
    }
}