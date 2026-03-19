package com.example.aifitnessapp.ui.log;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import com.example.aifitnessapp.R;
import com.example.aifitnessapp.data.model.ExerciseEntry;
import com.example.aifitnessapp.engine.ExerciseParser;
import com.example.aifitnessapp.ui.home.HomeActivity;
import com.example.aifitnessapp.viewmodel.LogViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogActivity extends AppCompatActivity {

    private LogViewModel viewModel;

    // Intent extras
    private int    plannedWorkoutId = -1;
    private String activityType     = "";
    private String sessionTitle     = "";
    private String sessionDetail    = "";

    // Exercise checklist
    private List<ExerciseEntry> exercises = new ArrayList<>();
    private LinearLayout exerciseListContainer;

    // Views
    private TextView    tvLogSessionTitle, tvLogActivityType;
    private RadioGroup  rgEffort;
    private View        sectionEffort;
    private TextInputEditText etUserNotes;
    private MaterialButton btnSaveLog, btnAddPhoto;
    private ImageView   ivPhotoPreview;
    private TextView    tvPhotoLabel;

    // Photo handling
    private String currentPhotoPath = null;
    private Uri    cameraImageUri   = null;

    private int currentUserId = -1;

    // Activity result launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        viewModel = new ViewModelProvider(this).get(LogViewModel.class);

        // Read intent extras
        plannedWorkoutId = getIntent().getIntExtra("plannedWorkoutId", -1);
        activityType     = getIntent().getStringExtra("activityType");
        sessionTitle     = getIntent().getStringExtra("sessionTitle");
        sessionDetail    = getIntent().getStringExtra("sessionDetail");
        if (activityType  == null) activityType  = "";
        if (sessionTitle  == null) sessionTitle  = "Today's Workout";
        if (sessionDetail == null) sessionDetail = "";

        // Parse exercises from session detail
        exercises = ExerciseParser.parse(sessionDetail);

        registerLaunchers();
        bindViews();
        populateHeader();
        buildExerciseList();
        setupPhotoButton();

        // Hide effort for rest days
        if ("REST".equals(activityType)) {
            sectionEffort.setVisibility(View.GONE);
        }

        // Get userId
        viewModel.currentUser.observe(this, user -> {
            if (user != null) currentUserId = user.id;
        });

        // Observe photo path update
        viewModel.photoPath.observe(this, path -> {
            if (path != null) {
                currentPhotoPath = path;
                ivPhotoPreview.setVisibility(View.VISIBLE);
                ivPhotoPreview.setImageURI(Uri.parse(path));
                tvPhotoLabel.setText("📷 Photo saved — tap to change");
            }
        });

        // Save
        btnSaveLog.setOnClickListener(v -> {
            if (!validate()) return;
            saveLog();
        });

        // Observe save success
        viewModel.saveSuccess.observe(this, success -> {
            if (success == null || !success) return;
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnBackFromLog).setOnClickListener(v -> finish());
    }

    // ── Exercise checklist ────────────────────────────────────

    private void buildExerciseList() {
        exerciseListContainer.removeAllViews();

        if (exercises.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No exercises planned — add your own below.");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(13f);
            exerciseListContainer.addView(empty);
            return;
        }

        for (int i = 0; i < exercises.size(); i++) {
            addExerciseRow(exercises.get(i), i);
        }
    }

    private void addExerciseRow(ExerciseEntry entry, int index) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_exercise_row, exerciseListContainer, false);

        // Cast explicitly — avoids "cannot resolve symbol" if R not regenerated yet
        CheckBox  cbDone  = (CheckBox)  row.findViewById(R.id.cbExerciseDone);
        TextView  tvName  = (TextView)  row.findViewById(R.id.tvExerciseName);
        TextView  tvSets  = (TextView)  row.findViewById(R.id.tvExerciseSets);
        ImageView btnEdit = (ImageView) row.findViewById(R.id.btnEditExercise);

        if (cbDone == null || tvName == null || tvSets == null || btnEdit == null) {
            // Layout IDs not found — skip this row safely
            exerciseListContainer.addView(row);
            return;
        }

        cbDone.setChecked(entry.completed);
        tvName.setText(entry.name);
        tvSets.setText(entry.actual.isEmpty() ? "—" : entry.actual);

        cbDone.setOnCheckedChangeListener((btn, checked) -> {
            entry.completed = checked;
            tvName.setAlpha(checked ? 0.5f : 1f);
        });
        tvName.setAlpha(entry.completed ? 0.5f : 1f);

        btnEdit.setOnClickListener(v -> showEditDialog(entry, tvSets));

        exerciseListContainer.addView(row);
    }

    /*
     * Shows a simple AlertDialog with an EditText to let user
     * change the sets/reps for an exercise.
     * e.g. planned "4×5" → user types "3×5" if they did less
     */
    private void showEditDialog(ExerciseEntry entry, TextView tvSets) {
        TextInputEditText input = new TextInputEditText(this);
        input.setText(entry.actual);
        input.setHint("e.g. 3×5 or 4×8");
        input.setPadding(48, 32, 48, 32);
        input.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("Edit: " + entry.name)
                .setMessage("Planned: " + (entry.original.isEmpty() ? "—" : entry.original))
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = input.getText() != null
                            ? input.getText().toString().trim() : "";
                    entry.actual = val.isEmpty() ? entry.original : val;
                    tvSets.setText(entry.actual.isEmpty() ? "—" : entry.actual);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Add custom exercise button
    private void setupAddExercise() {
        findViewById(R.id.btnAddExercise).setOnClickListener(v -> {
            TextInputEditText nameInput = new TextInputEditText(this);
            nameInput.setHint("Exercise name (e.g. Pull-ups)");
            nameInput.setPadding(48, 32, 48, 16);

            TextInputEditText setsInput = new TextInputEditText(this);
            setsInput.setHint("Sets × reps (e.g. 3×10)");
            setsInput.setPadding(48, 16, 48, 32);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(nameInput);
            layout.addView(setsInput);

            new AlertDialog.Builder(this)
                    .setTitle("Add Exercise")
                    .setView(layout)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String name = nameInput.getText() != null
                                ? nameInput.getText().toString().trim() : "";
                        String sets = setsInput.getText() != null
                                ? setsInput.getText().toString().trim() : "";
                        if (!name.isEmpty()) {
                            ExerciseEntry entry = new ExerciseEntry(name, sets);
                            exercises.add(entry);
                            addExerciseRow(entry, exercises.size() - 1);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ── Photo handling ────────────────────────────────────────

    private void registerLaunchers() {
        // Camera result
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoPath != null) {
                        viewModel.photoPath.setValue("file://" + currentPhotoPath);
                    }
                });

        // Gallery result
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            viewModel.photoPath.setValue(uri.toString());
                        }
                    }
                });

        // Camera permission result
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) launchCamera();
                    else Toast.makeText(this,
                            "Camera permission needed to take photos",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupPhotoButton() {
        btnAddPhoto.setOnClickListener(v -> showPhotoSourceDialog());
    }

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(new String[]{"📷 Take Photo", "🖼️ Choose from Gallery"},
                        (dialog, which) -> {
                            if (which == 0) checkCameraPermission();
                            else            launchGallery();
                        })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Could not create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    /*
     * Creates a uniquely named image file in the app's pictures directory.
     * Using app-private storage — no WRITE_EXTERNAL_STORAGE permission needed.
     */
    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String fileName  = "WORKOUT_" + timestamp + "_";
        File storageDir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image       = File.createTempFile(fileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // ── Save ──────────────────────────────────────────────────

    private boolean validate() {
        if (currentUserId == -1) {
            Toast.makeText(this, "User not loaded, try again", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (sectionEffort.getVisibility() == View.VISIBLE
                && rgEffort.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Rate how hard it felt (1–5)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveLog() {
        // Determine completion status from exercise ticks
        long done  = 0;
        for (ExerciseEntry e : exercises) if (e.completed) done++;
        String status;
        if (exercises.isEmpty()) {
            status = "COMPLETED";
        } else if (done == 0) {
            status = "SKIPPED";
        } else if (done < exercises.size()) {
            status = "MODIFIED";
        } else {
            status = "COMPLETED";
        }

        // Effort
        int effort = 0;
        if (sectionEffort.getVisibility() == View.VISIBLE) {
            int effortId = rgEffort.getCheckedRadioButtonId();
            if      (effortId == R.id.rbEff1) effort = 1;
            else if (effortId == R.id.rbEff2) effort = 2;
            else if (effortId == R.id.rbEff3) effort = 3;
            else if (effortId == R.id.rbEff4) effort = 4;
            else if (effortId == R.id.rbEff5) effort = 5;
        }

        // Combine exercise log + user notes
        String exerciseLog = ExerciseParser.serialize(exercises);
        String userNotes   = etUserNotes.getText() != null
                ? etUserNotes.getText().toString().trim() : "";
        String fullNotes   = exerciseLog.isEmpty() ? userNotes
                : exerciseLog + (userNotes.isEmpty() ? "" : "\n\n" + userNotes);

        viewModel.saveLog(currentUserId, plannedWorkoutId,
                status, effort, fullNotes, currentPhotoPath);
    }

    // ── View binding ──────────────────────────────────────────

    private void bindViews() {
        tvLogSessionTitle      = findViewById(R.id.tvLogSessionTitle);
        tvLogActivityType      = findViewById(R.id.tvLogActivityType);
        exerciseListContainer  = findViewById(R.id.exerciseListContainer);
        rgEffort               = findViewById(R.id.rgEffort);
        sectionEffort          = findViewById(R.id.sectionEffort);
        etUserNotes            = findViewById(R.id.etNotes);
        btnSaveLog             = findViewById(R.id.btnSaveLog);
        btnAddPhoto            = findViewById(R.id.btnAddPhoto);
        ivPhotoPreview         = findViewById(R.id.ivPhotoPreview);
        tvPhotoLabel           = findViewById(R.id.tvPhotoLabel);

        setupAddExercise();
    }

    private void populateHeader() {
        tvLogSessionTitle.setText(sessionTitle);
        tvLogActivityType.setText(activityEmoji(activityType)
                + "  " + formatType(activityType));
    }

    private String activityEmoji(String type) {
        if (type == null) return "💪";
        switch (type) {
            case "GYM":        return "🏋️";
            case "RUNNING":    return "🏃";
            case "BOULDERING": return "🧗";
            case "CYCLING":    return "🚴";
            case "SWIMMING":   return "🏊";
            case "YOGA":       return "🧘";
            case "HOME":       return "🏠";
            case "SPORTS":     return "⚽";
            case "REST":       return "😴";
            default:           return "💪";
        }
    }

    private String formatType(String type) {
        if (type == null) return "Workout";
        switch (type) {
            case "GYM":        return "Gym";
            case "RUNNING":    return "Running";
            case "BOULDERING": return "Bouldering";
            case "CYCLING":    return "Cycling";
            case "SWIMMING":   return "Swimming";
            case "YOGA":       return "Yoga";
            case "HOME":       return "Home Workout";
            case "SPORTS":     return "Sports";
            case "REST":       return "Rest Day";
            default:           return type;
        }
    }
}