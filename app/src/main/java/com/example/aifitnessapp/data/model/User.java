package com.example.aifitnessapp.data.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/*
 * User — authentication entity.
 *
 * SEPARATE FROM UserPreferences intentionally:
 *  - UserPreferences = fitness profile (goal, activities, weight)
 *  - User            = identity (email, password hash, timestamps)
 *
 * In Phase 2C, this table's data moves to the server.
 * UserPreferences stays local but syncs to the backend.
 *
 * INDEX on email:
 * We look up users by email frequently (login check).
 * An index makes that lookup O(log n) instead of O(n) —
 * much faster as the user table grows.
 * unique = true enforces that no two accounts share an email.
 */
@Entity(
        tableName = "users",
        indices = {@Index(value = "email", unique = true)}
)
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Email — the login identifier.
    // Stored in lowercase always (normalised in AuthRepository).
    // unique index enforced at the database level.
    public String email;

    /*
     * Password hash — SHA-256 of the user's password.
     * We NEVER store the raw password.
     *
     * In Phase 2C: this field becomes unused locally.
     * Authentication moves to the server (bcrypt + JWT).
     * We'll keep the field but stop writing to it.
     */
    public String passwordHash;

    /*
     * Local session token — a randomly generated UUID stored here
     * and in SharedPreferences after login.
     *
     * On login: generate UUID, store in both User row + SharedPreferences.
     * On logout: clear SharedPreferences. Token in DB stays (audit trail).
     * On next login: generate a fresh UUID.
     *
     * In Phase 2C: replaced by a JWT from the FastAPI server.
     * SessionManager.saveSession() will store the JWT instead of UUID.
     */
    public String sessionToken;

    // Timestamps — ISO format "yyyy-MM-dd HH:mm:ss"
    public String createdAt;
    public String lastLoginAt;

    /*
     * Link to UserPreferences.
     * After registration + onboarding, we store the UserPreferences id
     * here so we can load the right profile for this user on login.
     *
     * Default -1 means onboarding hasn't happened yet.
     * Set to the UserPreferences.id after onboarding completes.
     */
    public int preferencesId;
}