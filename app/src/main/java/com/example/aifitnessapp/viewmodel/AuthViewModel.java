package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.repository.AuthRepository;

/*
 * AuthViewModel — state holder for AuthActivity.
 *
 * Survives screen rotation (like all ViewModels).
 * This matters because auth operations run on background threads —
 * if the user rotates mid-login, we don't lose the result.
 *
 * LIVEDATA FIELDS:
 *  authState   → drives navigation (LOGIN_SUCCESS, REGISTER_SUCCESS, ERROR)
 *  errorMessage → shown in the UI when authState == ERROR
 *  isLoading   → disables buttons and shows spinner during operations
 *  loggedInUserId → passed to OnboardingActivity / HomeActivity after success
 */
public class AuthViewModel extends AndroidViewModel {

    // ── Auth state constants ──────────────────────────────────
    // Using String constants instead of an enum keeps it simple.
    // In a larger app you'd use a sealed class (Kotlin) or enum.
    public static final String STATE_IDLE             = "IDLE";
    public static final String STATE_LOGIN_SUCCESS    = "LOGIN_SUCCESS";
    public static final String STATE_REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String STATE_ERROR            = "ERROR";

    // ── Observable state ──────────────────────────────────────
    public final MutableLiveData<String>  authState      = new MutableLiveData<>(STATE_IDLE);
    public final MutableLiveData<String>  errorMessage   = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> isLoading      = new MutableLiveData<>(false);
    public final MutableLiveData<Integer> loggedInUserId = new MutableLiveData<>(-1);

    private final AuthRepository authRepo;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepo = new AuthRepository(application);
    }

    // ─────────────────────────────────────────────────────────
    //  REGISTER
    // ─────────────────────────────────────────────────────────
    public void register(String email, String password, String confirmPassword) {

        // Validate confirm password BEFORE calling the repository.
        // This check is pure UI logic — no DB needed.
        if (!password.equals(confirmPassword)) {
            errorMessage.setValue("Passwords do not match.");
            authState.setValue(STATE_ERROR);
            return;
        }

        isLoading.setValue(true);
        authState.setValue(STATE_IDLE);

        authRepo.register(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(int userId) {
                // postValue() because this runs on background thread
                loggedInUserId.postValue(userId);
                isLoading.postValue(false);
                authState.postValue(STATE_REGISTER_SUCCESS);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
                authState.postValue(STATE_ERROR);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────────────────
    public void login(String email, String password) {
        if (email.trim().isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Please enter your email and password.");
            authState.setValue(STATE_ERROR);
            return;
        }

        isLoading.setValue(true);
        authState.setValue(STATE_IDLE);

        authRepo.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(int userId) {
                loggedInUserId.postValue(userId);
                isLoading.postValue(false);
                authState.postValue(STATE_LOGIN_SUCCESS);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
                authState.postValue(STATE_ERROR);
            }
        });
    }

    // ── Reset state after navigation ──────────────────────────
    // Called after AuthActivity handles a success state,
    // so re-subscribing observers don't re-trigger navigation.
    public void resetState() {
        authState.setValue(STATE_IDLE);
        errorMessage.setValue("");
    }
}