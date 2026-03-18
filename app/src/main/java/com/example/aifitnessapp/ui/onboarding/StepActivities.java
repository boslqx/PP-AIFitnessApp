package com.example.aifitnessapp.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.viewmodel.OnboardingViewModel;
import java.util.ArrayList;
import java.util.List;

public class StepActivities implements OnboardingStep {

    private final Context context;
    private final OnboardingViewModel viewModel;

    private CheckBox cbGym, cbRunning, cbBouldering, cbCycling,
            cbSwimming, cbYoga, cbHome, cbSports;

    public StepActivities(Context context, OnboardingViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    @Override public String getTitle()    { return "What activities do you enjoy?"; }
    @Override public String getSubtitle() { return "Pick everything you like — your plan will mix them."; }

    @Override
    public void show(FrameLayout container) {
        container.removeAllViews();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.step_activities, container, true);

        cbGym        = view.findViewById(R.id.cbGym);
        cbRunning    = view.findViewById(R.id.cbRunning);
        cbBouldering = view.findViewById(R.id.cbBouldering);
        cbCycling    = view.findViewById(R.id.cbCycling);
        cbSwimming   = view.findViewById(R.id.cbSwimming);
        cbYoga       = view.findViewById(R.id.cbYoga);
        cbHome       = view.findViewById(R.id.cbHome);
        cbSports     = view.findViewById(R.id.cbSports);

        // Restore previous selections
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft != null && draft.selectedActivities != null) {
            cbGym.setChecked(draft.hasActivity("GYM"));
            cbRunning.setChecked(draft.hasActivity("RUNNING"));
            cbBouldering.setChecked(draft.hasActivity("BOULDERING"));
            cbCycling.setChecked(draft.hasActivity("CYCLING"));
            cbSwimming.setChecked(draft.hasActivity("SWIMMING"));
            cbYoga.setChecked(draft.hasActivity("YOGA"));
            cbHome.setChecked(draft.hasActivity("HOME"));
            cbSports.setChecked(draft.hasActivity("SPORTS"));
        }
    }

    @Override
    public boolean validate() {
        if (!cbGym.isChecked() && !cbRunning.isChecked() && !cbBouldering.isChecked()
                && !cbCycling.isChecked() && !cbSwimming.isChecked()
                && !cbYoga.isChecked() && !cbHome.isChecked() && !cbSports.isChecked()) {
            Toast.makeText(context, "Pick at least one activity", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveToProfile() {
        UserPreferences draft = viewModel.draftProfile.getValue();
        if (draft == null) return;

        List<String> selected = new ArrayList<>();
        if (cbGym.isChecked())        selected.add("GYM");
        if (cbRunning.isChecked())    selected.add("RUNNING");
        if (cbBouldering.isChecked()) selected.add("BOULDERING");
        if (cbCycling.isChecked())    selected.add("CYCLING");
        if (cbSwimming.isChecked())   selected.add("SWIMMING");
        if (cbYoga.isChecked())       selected.add("YOGA");
        if (cbHome.isChecked())       selected.add("HOME");
        if (cbSports.isChecked())     selected.add("SPORTS");

        draft.selectedActivities = String.join(",", selected);
        viewModel.draftProfile.setValue(draft);
    }
}