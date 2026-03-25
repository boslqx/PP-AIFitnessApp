package com.example.aifitnessapp.repository;

import android.app.Application;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.db.dao.UserDao;
import com.example.aifitnessapp.data.model.User;
import com.example.aifitnessapp.util.AppExecutors;
import com.example.aifitnessapp.util.SessionManager;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/*
 * AuthRepository — all authentication operations.
 *
 * DESIGNED FOR PHASE 2C SWAP:
 * Every public method currently writes to Room (local).
 * In Phase 2C, replace the body of each method with a Retrofit
 * HTTP call to the FastAPI server. The callback signatures stay
 * identical — AuthViewModel and AuthActivity change nothing.
 *
 * THREAD SAFETY:
 * All database operations run on AppExecutors.diskIO().
 * Results are delivered via AuthCallback on that same thread.
 * The ViewModel posts results to LiveData using postValue(),
 * which is thread-safe and automatically switches to main thread.
 */
public class AuthRepository {

    // ── Callback interface ────────────────────────────────────
    /*
     * Two outcomes for every auth operation: success or failure.
     * onSuccess carries the userId so the ViewModel can store it.
     * onError carries a user-facing message string.
     *
     * In Phase 2C: onSuccess will also carry a JWT token string.
     * We'll add that parameter then — no other changes needed.
     */
    public interface AuthCallback {
        void onSuccess(int userId);
        void onError(String message);
    }

    private final FitAIDatabase db;
    private final UserDao       userDao;
    private final SessionManager session;

    public AuthRepository(Application application) {
        this.db      = FitAIDatabase.getInstance(application);
        this.userDao = db.userDao();
        this.session = new SessionManager(application);
    }

    // ─────────────────────────────────────────────────────────
    //  REGISTER
    //
    //  Steps:
    //  1. Validate email format
    //  2. Check email not already registered
    //  3. Hash password with SHA-256
    //  4. Insert User row into Room
    //  5. Save session to SharedPreferences
    //  6. Call back with the new userId
    // ─────────────────────────────────────────────────────────
    public void register(String email, String password, AuthCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            // Normalise email to lowercase — "User@Email.com" == "user@email.com"
            String normEmail = email.trim().toLowerCase(Locale.getDefault());

            // Step 1: Basic email validation
            if (!isValidEmail(normEmail)) {
                callback.onError("Please enter a valid email address.");
                return;
            }

            // Step 2: Password length check
            if (password.length() < 6) {
                callback.onError("Password must be at least 6 characters.");
                return;
            }

            // Step 3: Check for duplicate email
            User existing = userDao.findByEmail(normEmail);
            if (existing != null) {
                callback.onError("An account with this email already exists.");
                return;
            }

            // Step 4: Hash password
            String hash = hashPassword(password);
            if (hash == null) {
                callback.onError("Registration failed — please try again.");
                return;
            }

            // Step 5: Build User object
            String now   = timestamp();
            String token = UUID.randomUUID().toString();
            // UUID.randomUUID() generates a universally unique 128-bit string
            // e.g. "550e8400-e29b-41d4-a716-446655440000"
            // Collision probability is astronomically low — safe as a session token

            User user         = new User();
            user.email        = normEmail;
            user.passwordHash = hash;
            user.sessionToken = token;
            user.createdAt    = now;
            user.lastLoginAt  = now;
            user.preferencesId = -1; // set after onboarding completes

            // Step 6: Insert into Room
            // insert() returns the new row's id (auto-generated primary key)
            long newId;
            try {
                newId = userDao.insert(user);
            } catch (android.database.sqlite.SQLiteConstraintException e) {
                // Race condition: another thread inserted same email between
                // our check and our insert. Extremely unlikely but handled.
                callback.onError("An account with this email already exists.");
                return;
            }

            // Step 7: Save session
            session.saveSession((int) newId, normEmail, token);

            callback.onSuccess((int) newId);
        });
    }

    // ─────────────────────────────────────────────────────────
    //  LOGIN
    //
    //  Steps:
    //  1. Normalise email
    //  2. Find User row by email
    //  3. Hash the entered password and compare to stored hash
    //  4. Generate new session token, update DB
    //  5. Save session to SharedPreferences
    //  6. Call back with userId
    // ─────────────────────────────────────────────────────────
    public void login(String email, String password, AuthCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {

            String normEmail = email.trim().toLowerCase(Locale.getDefault());

            // Step 1: Find account
            User user = userDao.findByEmail(normEmail);
            if (user == null) {
                // Deliberately vague — don't tell attacker if email exists
                callback.onError("Incorrect email or password.");
                return;
            }

            // Step 2: Verify password
            String enteredHash = hashPassword(password);
            if (enteredHash == null || !enteredHash.equals(user.passwordHash)) {
                callback.onError("Incorrect email or password.");
                return;
            }

            // Step 3: Generate fresh session token
            String newToken = UUID.randomUUID().toString();
            String now      = timestamp();

            // Step 4: Update session in DB
            userDao.updateSession(user.id, newToken, now);

            // Step 5: Save session to SharedPreferences
            session.saveSession(user.id, normEmail, newToken);

            callback.onSuccess(user.id);
        });
    }

    // ─────────────────────────────────────────────────────────
    //  LOGOUT
    //
    //  Clears SharedPreferences session.
    //  Does NOT delete the User row — account remains intact.
    //  Synchronous — safe to call from main thread (no DB access).
    // ─────────────────────────────────────────────────────────
    public void logout() {
        session.logout();
    }

    // ─────────────────────────────────────────────────────────
    //  LINK PREFERENCES
    //
    //  Called after onboarding completes.
    //  Stores the UserPreferences.id in the User row so we can
    //  load the right fitness profile when the user logs back in.
    // ─────────────────────────────────────────────────────────
    public void linkPreferences(int userId, int prefsId) {
        AppExecutors.getInstance().diskIO().execute(() ->
                userDao.updatePreferencesId(userId, prefsId));
    }

    // ─────────────────────────────────────────────────────────
    //  SESSION CHECK — called by MainActivity
    // ─────────────────────────────────────────────────────────
    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    public int getLoggedInUserId() {
        return session.getUserId();
    }

    public String getLoggedInEmail() {
        return session.getUserEmail();
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    /*
     * SHA-256 password hashing.
     *
     * HOW IT WORKS:
     * 1. MessageDigest.getInstance("SHA-256") — get the SHA-256 algorithm
     * 2. digest(input.getBytes()) — run the hash function, returns byte[]
     * 3. Convert each byte to 2-character hex — produces 64-char string
     *
     * The byte-to-hex loop:
     *   (b & 0xff) — converts signed byte (-128 to 127) to unsigned int (0-255)
     *   Integer.toHexString() — converts to hex (0-ff)
     *   sb.append("0") guard — ensures single-digit hex gets a leading zero
     *   e.g. byte value 5 → "05" not "5"
     *
     * Returns null only if SHA-256 is unavailable — impossible on Android.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) sb.append("0");
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * Simple email format validation.
     * Checks for the presence of @ and a dot after it.
     * Not exhaustive — just catches obvious typos.
     * In Phase 2C the server validates email more rigorously.
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        int atIndex  = email.indexOf('@');
        if (atIndex < 1) return false;             // nothing before @
        int dotIndex = email.lastIndexOf('.');
        return dotIndex > atIndex + 1             // dot after @
                && dotIndex < email.length() - 1;     // not the last char
    }

    private String timestamp() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}