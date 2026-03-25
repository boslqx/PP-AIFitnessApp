package com.example.aifitnessapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * SessionManager — wraps SharedPreferences for login session management.
 *
 * WHAT IT STORES:
 *  - isLoggedIn  (boolean) — is a user currently logged in?
 *  - userId      (int)     — the logged-in user's Room primary key
 *  - userEmail   (String)  — displayed in Settings header
 *  - sessionToken(String)  — local UUID (Phase 2C: replaced by JWT)
 *
 * DESIGN FOR PHASE 2C:
 * In Phase 2C, saveSession() will receive a JWT string from the FastAPI
 * server instead of a local UUID. The rest of the app doesn't change —
 * only this class changes. That's the value of encapsulation.
 *
 * Usage:
 *   SessionManager session = new SessionManager(context);
 *   session.saveSession(userId, email, token);
 *   session.isLoggedIn() → true
 *   session.getUserId()  → 1
 *   session.logout()     → clears all session data
 */
public class SessionManager {

    // SharedPreferences file name — stable identifier for this file
    private static final String PREFS_NAME = "fitai_session";

    // Keys — string constants prevent typos across the codebase
    private static final String KEY_IS_LOGGED_IN   = "is_logged_in";
    private static final String KEY_USER_ID         = "user_id";
    private static final String KEY_USER_EMAIL      = "user_email";
    private static final String KEY_SESSION_TOKEN   = "session_token";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        // Application context prevents memory leaks from holding
        // an Activity context longer than the Activity lives
        prefs  = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ── SAVE SESSION (called after successful login or register) ──

    /*
     * Saves all session data atomically using apply().
     *
     * apply() vs commit():
     * commit() writes synchronously and returns a boolean success flag.
     * apply() writes asynchronously (fast) and doesn't return a result.
     * For session data that doesn't need confirmation, apply() is correct.
     * For critical financial data, you'd use commit().
     *
     * In Phase 2C: token will be a JWT string like "eyJhbGci..."
     */
    public void saveSession(int userId, String email, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt    (KEY_USER_ID,      userId);
        editor.putString (KEY_USER_EMAIL,   email);
        editor.putString (KEY_SESSION_TOKEN, token);
        editor.apply();
    }

    // ── READ SESSION ──────────────────────────────────────────

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        // Default false: if key doesn't exist, user is not logged in
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
        // Default -1: sentinel value meaning "no user loaded"
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getSessionToken() {
        return prefs.getString(KEY_SESSION_TOKEN, "");
    }

    // ── LOGOUT ───────────────────────────────────────────────

    /*
     * Clears all session data.
     * After this, isLoggedIn() returns false → MainActivity routes
     * the user back to AuthActivity.
     *
     * Note: this does NOT delete the User row from Room.
     * The account still exists — the user can log back in.
     * "Log out" ≠ "Delete account".
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}