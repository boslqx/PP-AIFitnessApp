package com.example.aifitnessapp.data.db.dao;

import androidx.room.*;
import com.example.aifitnessapp.data.model.User;

/*
 * UserDao — database operations for the User authentication table.
 *
 * All methods are SYNCHRONOUS (no LiveData) because:
 * 1. Auth is a one-shot operation, not an ongoing stream
 * 2. AuthRepository runs all calls on AppExecutors.diskIO()
 * 3. We need the result immediately to decide what to do next
 *
 * ABORT conflict strategy on insert:
 * If the email already exists (unique index), Room throws an exception
 * instead of silently replacing. We catch this in AuthRepository
 * and show "Email already registered" to the user.
 */
@Dao
public interface UserDao {

    // ── INSERT ────────────────────────────────────────────────
    /*
     * Returns the new row's id (long) — we use this to immediately
     * know the userId after registration, without a second query.
     * ABORT: throws SQLiteConstraintException if email is duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);

    // ── UPDATE ────────────────────────────────────────────────
    @Update
    void update(User user);

    // ── SELECT ────────────────────────────────────────────────

    // Find user by email for login verification.
    // Returns null if no account with that email exists.
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    // Find user by their primary key.
    // Used after login to load the full User object.
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(int id);

    // Count all users — used by MainActivity to check if
    // anyone has registered yet (determines first-launch flow).
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    // ── SESSION ───────────────────────────────────────────────

    // Update the session token + last login timestamp after successful login.
    // Using a targeted @Query instead of @Update so we only touch
    // these two columns — not the email, password hash, etc.
    @Query("UPDATE users SET sessionToken = :token, lastLoginAt = :timestamp WHERE id = :userId")
    void updateSession(int userId, String token, String timestamp);

    // Link user to their onboarding preferences after onboarding completes.
    @Query("UPDATE users SET preferencesId = :prefsId WHERE id = :userId")
    void updatePreferencesId(int userId, int prefsId);
}