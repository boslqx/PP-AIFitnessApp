package com.example.aifitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.onboarding.OnboardingActivity;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AuthActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    // ── Tab buttons ───────────────────────────────────────────
    private MaterialButton btnTabLogin, btnTabRegister;

    // ── Panels ────────────────────────────────────────────────
    private LinearLayout loginPanel, registerPanel;

    // ── Login fields ──────────────────────────────────────────
    private TextInputEditText etLoginEmail, etLoginPassword;
    private MaterialButton    btnLogin;

    // ── Register fields ───────────────────────────────────────
    private TextInputEditText etRegisterEmail, etRegisterPassword, etRegisterConfirm;
    private MaterialButton    btnRegister;

    // ── Shared ────────────────────────────────────────────────
    private TextView    tvAuthError;
    private ProgressBar pbAuth;

    // Track which tab is active — login by default
    private boolean isLoginTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        bindViews();
        setupTabs();
        setupButtons();
        observeViewModel();
    }

    // ─────────────────────────────────────────────────────────
    //  TAB SWITCHING
    //
    //  We toggle visibility of loginPanel / registerPanel.
    //  We also swap the button styles to show which tab is active:
    //  Active tab   = filled (Widget.Material3.Button)
    //  Inactive tab = outlined (Widget.Material3.Button.OutlinedButton)
    //
    //  We can't change a MaterialButton's style at runtime directly,
    //  so instead we change the background tint and stroke manually.
    //  The active tab gets the primary color background,
    //  inactive gets transparent background + primary stroke.
    // ─────────────────────────────────────────────────────────
    private void setupTabs() {
        btnTabLogin.setOnClickListener(v -> showLoginTab());
        btnTabRegister.setOnClickListener(v -> showRegisterTab());
    }

    private void showLoginTab() {
        isLoginTab = true;
        loginPanel.setVisibility(View.VISIBLE);
        registerPanel.setVisibility(View.GONE);
        tvAuthError.setVisibility(View.GONE);

        // Active style: filled
        btnTabLogin.setBackgroundTintList(
                getColorStateList(R.color.md_theme_primary));
        btnTabLogin.setTextColor(getColor(R.color.md_theme_onPrimary));
        btnTabLogin.setStrokeWidth(0);

        // Inactive style: outlined
        btnTabRegister.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.TRANSPARENT));
        btnTabRegister.setTextColor(getColor(R.color.md_theme_primary));
        btnTabRegister.setStrokeWidth(2);
        btnTabRegister.setStrokeColor(
                getColorStateList(R.color.md_theme_primary));
    }

    private void showRegisterTab() {
        isLoginTab = false;
        loginPanel.setVisibility(View.GONE);
        registerPanel.setVisibility(View.VISIBLE);
        tvAuthError.setVisibility(View.GONE);

        // Active style: filled
        btnTabRegister.setBackgroundTintList(
                getColorStateList(R.color.md_theme_primary));
        btnTabRegister.setTextColor(getColor(R.color.md_theme_onPrimary));
        btnTabRegister.setStrokeWidth(0);

        // Inactive style: outlined
        btnTabLogin.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.TRANSPARENT));
        btnTabLogin.setTextColor(getColor(R.color.md_theme_primary));
        btnTabLogin.setStrokeWidth(2);
        btnTabLogin.setStrokeColor(
                getColorStateList(R.color.md_theme_primary));
    }

    // ─────────────────────────────────────────────────────────
    //  BUTTON SETUP
    // ─────────────────────────────────────────────────────────
    private void setupButtons() {
        btnLogin.setOnClickListener(v -> {
            String email    = getText(etLoginEmail);
            String password = getText(etLoginPassword);
            viewModel.login(email, password);
        });

        btnRegister.setOnClickListener(v -> {
            String email    = getText(etRegisterEmail);
            String password = getText(etRegisterPassword);
            String confirm  = getText(etRegisterConfirm);
            viewModel.register(email, password, confirm);
        });
    }

    // ─────────────────────────────────────────────────────────
    //  OBSERVE VIEWMODEL
    //
    //  Three things to observe:
    //  1. isLoading  → show/hide spinner, enable/disable buttons
    //  2. authState  → navigate on success, show error on failure
    //  3. errorMessage → display the error text
    // ─────────────────────────────────────────────────────────
    private void observeViewModel() {

        // Loading state
        viewModel.isLoading.observe(this, loading -> {
            pbAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
            // Disable buttons during load to prevent double-tapping
            btnLogin.setEnabled(!loading);
            btnRegister.setEnabled(!loading);
            btnTabLogin.setEnabled(!loading);
            btnTabRegister.setEnabled(!loading);
        });

        // Auth state — drives navigation
        viewModel.authState.observe(this, state -> {
            if (state == null) return;

            switch (state) {

                case AuthViewModel.STATE_REGISTER_SUCCESS:
                    // New account created — go to onboarding
                    // Pass userId so OnboardingViewModel can link prefs to user
                    int newUserId = viewModel.loggedInUserId.getValue() != null
                            ? viewModel.loggedInUserId.getValue() : -1;
                    viewModel.resetState();
                    Intent onboard = new Intent(this, OnboardingActivity.class);
                    onboard.putExtra("userId", newUserId);
                    startActivity(onboard);
                    finish();
                    break;

                case AuthViewModel.STATE_LOGIN_SUCCESS:
                    // Existing user — go to home (weekly check happens in MainActivity)
                    viewModel.resetState();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    break;

                case AuthViewModel.STATE_ERROR:
                    // Show error message, stay on screen
                    String msg = viewModel.errorMessage.getValue();
                    if (msg != null && !msg.isEmpty()) {
                        tvAuthError.setText(msg);
                        tvAuthError.setVisibility(View.VISIBLE);
                    }
                    break;

                case AuthViewModel.STATE_IDLE:
                default:
                    tvAuthError.setVisibility(View.GONE);
                    break;
            }
        });
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        btnTabLogin      = findViewById(R.id.btnTabLogin);
        btnTabRegister   = findViewById(R.id.btnTabRegister);
        loginPanel       = findViewById(R.id.loginPanel);
        registerPanel    = findViewById(R.id.registerPanel);
        etLoginEmail     = findViewById(R.id.etLoginEmail);
        etLoginPassword  = findViewById(R.id.etLoginPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        etRegisterEmail  = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirm  = findViewById(R.id.etRegisterConfirm);
        btnRegister      = findViewById(R.id.btnRegister);
        tvAuthError      = findViewById(R.id.tvAuthError);
        pbAuth           = findViewById(R.id.pbAuth);
    }

    // ── Helper ────────────────────────────────────────────────

    private String getText(TextInputEditText field) {
        return field.getText() != null
                ? field.getText().toString().trim() : "";
    }
}