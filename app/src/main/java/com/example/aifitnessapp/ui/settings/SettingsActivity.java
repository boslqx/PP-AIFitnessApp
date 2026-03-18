package com.example.aifitnessapp.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.SettingsViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private SettingsViewModel viewModel;

    // Form fields
    private TextInputEditText etName, etAge, etHeight, etWeight, etTargetWeight;
    private RadioGroup rgGender, rgActivity, rgGoal, rgTimeline;
    private TextView tvNewTargets;

    // Holds the original profile so we preserve id + createdAt
    private UserProfile originalProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        bindViews();

        // Populate form with current profile values
        viewModel.currentUser.observe(this, user -> {
            if (user == null || originalProfile != null) return;
            // Only populate once — don't overwrite user edits on re-observation
            originalProfile = user;
            populateForm(user);
        });

        // Live preview: recalculate whenever any RadioGroup changes
        setupLivePreview();

        // Observe preview string → update green card
        viewModel.targetPreview.observe(this, preview -> {
            tvNewTargets.setText(preview);
        });

        // Save button
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            if (!validate()) return;
            UserProfile updated = buildUpdatedProfile();
            viewModel.saveSettings(updated);
        });

        // Observe save success
        viewModel.saveSuccess.observe(this, success -> {
            if (success == null || !success) return; {
                Toast.makeText(this,
                        "Settings saved! Targets recalculated. ✅",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
        });

        findViewById(R.id.btnBackFromSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void bindViews() {
        etName         = findViewById(R.id.etSettingsName);
        etAge          = findViewById(R.id.etSettingsAge);
        etHeight       = findViewById(R.id.etSettingsHeight);
        etWeight       = findViewById(R.id.etSettingsWeight);
        etTargetWeight = findViewById(R.id.etSettingsTargetWeight);
        rgGender       = findViewById(R.id.rgSettingsGender);
        rgActivity     = findViewById(R.id.rgSettingsActivity);
        rgGoal         = findViewById(R.id.rgSettingsGoal);
        rgTimeline     = findViewById(R.id.rgSettingsTimeline);
        tvNewTargets   = findViewById(R.id.tvNewTargets);
    }

    /*
     * Pre-fills every form field from the saved UserProfile.
     * Restores RadioGroup selections by matching tag values.
     */
    private void populateForm(UserProfile user) {
        etName.setText(user.name);
        etAge.setText(String.valueOf(user.age));
        etHeight.setText(String.valueOf(user.height));
        etWeight.setText(String.valueOf(user.weight));
        etTargetWeight.setText(String.valueOf(user.targetWeight));

        checkRadioByTag(rgGender,   user.gender);
        checkRadioByTag(rgActivity, user.activityLevel);
        checkRadioByTag(rgGoal,     user.fitnessGoal);
        checkRadioByTag(rgTimeline, String.valueOf(user.timelineMonths));

        // Trigger initial preview
        triggerPreview();
    }

    /*
     * Attaches listeners to all RadioGroups so the preview card
     * updates live as the user makes changes.
     */
    private void setupLivePreview() {
        RadioGroup.OnCheckedChangeListener listener =
                (group, checkedId) -> triggerPreview();

        rgGender.setOnCheckedChangeListener(listener);
        rgActivity.setOnCheckedChangeListener(listener);
        rgGoal.setOnCheckedChangeListener(listener);

        // Also trigger on text field focus loss
        etWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) triggerPreview();
        });
        etHeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) triggerPreview();
        });
        etAge.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) triggerPreview();
        });
    }

    private void triggerPreview() {
        viewModel.previewTargets(
                getFloat(etWeight),
                getFloat(etHeight),
                getInt(etAge),
                getSelectedTag(rgGender),
                getSelectedTag(rgActivity),
                getSelectedTag(rgGoal)
        );
    }

    private boolean validate() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Required");
            return false;
        }
        if (getInt(etAge) < 10 || getInt(etAge) > 100) {
            etAge.setError("Enter a valid age (10–100)");
            return false;
        }
        if (getFloat(etWeight) < 20 || getFloat(etWeight) > 300) {
            etWeight.setError("Enter a valid weight (20–300 kg)");
            return false;
        }
        if (getFloat(etHeight) < 100 || getFloat(etHeight) > 250) {
            etHeight.setError("Enter a valid height (100–250 cm)");
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgActivity.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select activity level", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgGoal.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a goal", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgTimeline.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a timeline", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /*
     * Builds the updated UserProfile from form values.
     * Preserves the original id and createdAt so Room does an UPDATE
     * instead of inserting a duplicate row.
     */
    private UserProfile buildUpdatedProfile() {
        UserProfile p = new UserProfile();
        p.id           = originalProfile != null ? originalProfile.id : 1;
        p.createdAt    = originalProfile != null ? originalProfile.createdAt
                : System.currentTimeMillis();
        p.name         = etName.getText().toString().trim();
        p.age          = getInt(etAge);
        p.weight       = getFloat(etWeight);
        p.height       = getFloat(etHeight);
        p.targetWeight = getFloat(etTargetWeight);
        p.gender       = getSelectedTag(rgGender);
        p.activityLevel = getSelectedTag(rgActivity);
        p.fitnessGoal  = getSelectedTag(rgGoal);
        p.timelineMonths = getIntTag(rgTimeline);
        return p;
    }

    // ── Helpers ──────────────────────────────────────────────

    /*
     * Finds the RadioButton inside a RadioGroup whose android:tag
     * matches the given value and checks it.
     */
    private void checkRadioByTag(RadioGroup rg, String tagValue) {
        if (tagValue == null) return;
        for (int i = 0; i < rg.getChildCount(); i++) {
            android.view.View child = rg.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                if (tagValue.equals(rb.getTag())) {
                    rg.check(rb.getId());
                    return;
                }
            }
        }
    }

    private String getSelectedTag(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return null;
        RadioButton rb = findViewById(id);
        return rb.getTag() != null ? rb.getTag().toString() : null;
    }

    private int getIntTag(RadioGroup rg) {
        String tag = getSelectedTag(rg);
        if (tag == null) return 0;
        try { return Integer.parseInt(tag); } catch (Exception e) { return 0; }
    }

    private int getInt(TextInputEditText field) {
        String s = field.getText().toString().trim();
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private float getFloat(TextInputEditText field) {
        String s = field.getText().toString().trim();
        try { return Float.parseFloat(s); } catch (Exception e) { return 0f; }
    }
}