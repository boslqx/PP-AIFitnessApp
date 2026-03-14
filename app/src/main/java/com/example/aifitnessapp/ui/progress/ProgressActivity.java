package com.example.aifitnessapp.ui.progress;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.ui.dashboard.DashboardActivity;
import com.example.aifitnessapp.viewmodel.ProgressViewModel;
import java.util.List;

public class ProgressActivity extends AppCompatActivity {

    private ProgressViewModel viewModel;
    private ProgressChartView chartView;
    private TextView tvChartTitle, tvSummary;
    private TextView tvStatVal1, tvStatVal2, tvStatVal3;
    private TextView tvStatLabel1, tvStatLabel2, tvStatLabel3;
    private Button btnTabWeight, btnTabCalories, btnTabSleep;

    // Track which tab is active so we can re-render after data loads
    private String activeTab = "weight";

    // Colors for each metric
    private static final int COLOR_WEIGHT   = Color.parseColor("#2196F3"); // blue
    private static final int COLOR_CALORIES = Color.parseColor("#FF9800"); // orange
    private static final int COLOR_SLEEP    = Color.parseColor("#9C27B0"); // purple

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        bindViews();
        setupTabs();

        // Load data once we have the userId
        viewModel.currentUser.observe(this, user -> {
            if (user == null) return;
            viewModel.loadProgress(user.id);
        });

        // Observe all three datasets — re-render active tab when data arrives
        viewModel.weightValues.observe(this, v  -> { if (activeTab.equals("weight"))   renderWeight(); });
        viewModel.calorieValues.observe(this, v -> { if (activeTab.equals("calories")) renderCalories(); });
        viewModel.sleepValues.observe(this, v   -> { if (activeTab.equals("sleep"))    renderSleep(); });

        // Render weight tab by default
        renderWeight();

        findViewById(R.id.btnBackFromProgress).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void bindViews() {
        chartView      = findViewById(R.id.chartView);
        tvChartTitle   = findViewById(R.id.tvChartTitle);
        tvSummary      = findViewById(R.id.tvSummary);
        tvStatVal1     = findViewById(R.id.tvStatVal1);
        tvStatVal2     = findViewById(R.id.tvStatVal2);
        tvStatVal3     = findViewById(R.id.tvStatVal3);
        tvStatLabel1   = findViewById(R.id.tvStatLabel1);
        tvStatLabel2   = findViewById(R.id.tvStatLabel2);
        tvStatLabel3   = findViewById(R.id.tvStatLabel3);
        btnTabWeight   = findViewById(R.id.btnTabWeight);
        btnTabCalories = findViewById(R.id.btnTabCalories);
        btnTabSleep    = findViewById(R.id.btnTabSleep);
    }

    private void setupTabs() {
        btnTabWeight.setOnClickListener(v -> {
            activeTab = "weight";
            renderWeight();
        });
        btnTabCalories.setOnClickListener(v -> {
            activeTab = "calories";
            renderCalories();
        });
        btnTabSleep.setOnClickListener(v -> {
            activeTab = "sleep";
            renderSleep();
        });
    }

    // ── Render methods ────────────────────────────────────────

    private void renderWeight() {
        tvChartTitle.setText("Weight (kg)");
        highlightTab(btnTabWeight);

        List<Float>  vals = viewModel.weightValues.getValue();
        List<String> lbls = viewModel.weightLabels.getValue();
        chartView.setData(vals, lbls, "kg", COLOR_WEIGHT);

        String summary = viewModel.weightSummary.getValue();
        tvSummary.setText(summary != null ? summary : "Log your weight to see trends.");

        updateStatTiles(vals, "kg", "START", "LATEST", "CHANGE");
    }

    private void renderCalories() {
        tvChartTitle.setText("Calories (kcal)");
        highlightTab(btnTabCalories);

        List<Float>  vals = viewModel.calorieValues.getValue();
        List<String> lbls = viewModel.calorieLabels.getValue();
        chartView.setData(vals, lbls, "kcal", COLOR_CALORIES);

        String summary = viewModel.calorieSummary.getValue();
        tvSummary.setText(summary != null ? summary : "Log your calories to see trends.");

        updateStatTiles(vals, "kcal", "FIRST DAY", "LATEST", "AVG");
    }

    private void renderSleep() {
        tvChartTitle.setText("Sleep (hours)");
        highlightTab(btnTabSleep);

        List<Float>  vals = viewModel.sleepValues.getValue();
        List<String> lbls = viewModel.sleepLabels.getValue();
        chartView.setData(vals, lbls, "hrs", COLOR_SLEEP);

        String summary = viewModel.sleepSummary.getValue();
        tvSummary.setText(summary != null ? summary : "Log your sleep to see trends.");

        updateStatTiles(vals, "h", "FIRST DAY", "LATEST", "AVG");
    }

    /*
     * Fills the 3 stat tiles with start / latest / change values.
     * Works for any dataset.
     */
    private void updateStatTiles(List<Float> vals, String unit,
                                 String lbl1, String lbl2, String lbl3) {
        tvStatLabel1.setText(lbl1);
        tvStatLabel2.setText(lbl2);
        tvStatLabel3.setText(lbl3);

        if (vals == null || vals.isEmpty()) {
            tvStatVal1.setText("—");
            tvStatVal2.setText("—");
            tvStatVal3.setText("—");
            return;
        }

        float first  = vals.get(0);
        float latest = vals.get(vals.size() - 1);

        // For calories/sleep tile 3 = average, for weight tile 3 = change
        float tile3;
        String tile3Str;
        if (lbl3.equals("CHANGE")) {
            tile3 = latest - first;
            tile3Str = (tile3 >= 0 ? "+" : "") +
                    String.format("%.1f", tile3) + unit;
            tvStatVal3.setTextColor(tile3 < 0
                    ? Color.parseColor("#4CAF50")   // lost weight = green
                    : Color.parseColor("#EF5350")); // gained = red
        } else {
            float sum = 0;
            for (float v : vals) sum += v;
            tile3 = sum / vals.size();
            tile3Str = String.format("%.1f", tile3) + unit;
            tvStatVal3.setTextColor(Color.parseColor("#212121"));
        }

        tvStatVal1.setText(String.format("%.1f", first)  + unit);
        tvStatVal2.setText(String.format("%.1f", latest) + unit);
        tvStatVal3.setText(tile3Str);
    }

    // Highlight active tab with green text, others grey
    private void highlightTab(Button active) {
        int grey  = Color.parseColor("#9E9E9E");
        int green = Color.parseColor("#2E7D32");
        btnTabWeight.setTextColor(grey);
        btnTabCalories.setTextColor(grey);
        btnTabSleep.setTextColor(grey);
        active.setTextColor(green);
    }
}