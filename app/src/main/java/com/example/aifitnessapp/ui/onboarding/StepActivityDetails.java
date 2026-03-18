package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;

public class StepActivityDetails implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;

    // Gym section
    private LinearLayout sectionGym;
    private RadioGroup rgGymEquipment;

    // Running section
    private LinearLayout sectionRunning;
    private CheckBox cbTracksRunning;

    // Sports section
    private LinearLayout sectionSports;
    private EditText etSports;

    public StepActivityDetails(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "Tell us more"; }
    @Override public String getSubtitle() { return "Help us tailor workouts for your specific setup."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_activity_details, container, true);

        sectionGym      = view.findViewById(R.id.sectionGym);
        rgGymEquipment  = view.findViewById(R.id.rgGymEquipment);
        sectionRunning  = view.findViewById(R.id.sectionRunning);
        cbTracksRunning = view.findViewById(R.id.cbTracksRunning);
        sectionSports   = view.findViewById(R.id.sectionSports);
        etSports        = view.findViewById(R.id.etSports);

        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        // Show/hide sections based on selected activities
        sectionGym.setVisibility(draft.hasActivity("GYM") ? View.VISIBLE : View.GONE);
        sectionRunning.setVisibility(draft.hasActivity("RUNNING") ? View.VISIBLE : View.GONE);
        sectionSports.setVisibility(draft.hasActivity("SPORTS") ? View.VISIBLE : View.GONE);

        // Restore previous values
        if (draft.gymEquipment != null) {
            switch (draft.gymEquipment) {
                case "FULL_GYM":      rgGymEquipment.check(R.id.rbFullGym);     break;
                case "DUMBBELLS":     rgGymEquipment.check(R.id.rbDumbbells);   break;
                case "BARBELL_RACK":  rgGymEquipment.check(R.id.rbBarbellRack); break;
                case "MACHINES":      rgGymEquipment.check(R.id.rbMachines);    break;
            }
        }
        cbTracksRunning.setChecked(draft.tracksRunningPace);
        if (draft.sports != null) etSports.setText(draft.sports);
    }

    @Override
    public boolean validate() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return true;

        // If gym is selected, equipment must be chosen
        if (draft.hasActivity("GYM") && rgGymEquipment.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select your gym equipment", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        // Save gym equipment
        if (draft.hasActivity("GYM")) {
            int id = rgGymEquipment.getCheckedRadioButtonId();
            if (id == R.id.rbFullGym)     draft.gymEquipment = "FULL_GYM";
            if (id == R.id.rbDumbbells)   draft.gymEquipment = "DUMBBELLS";
            if (id == R.id.rbBarbellRack) draft.gymEquipment = "BARBELL_RACK";
            if (id == R.id.rbMachines)    draft.gymEquipment = "MACHINES";
        }

        draft.tracksRunningPace = cbTracksRunning.isChecked();

        if (draft.hasActivity("SPORTS")) {
            draft.sports = etSports.getText().toString().trim();
        }

        viewModel.draftProfile.setValue(draft);
    }
}