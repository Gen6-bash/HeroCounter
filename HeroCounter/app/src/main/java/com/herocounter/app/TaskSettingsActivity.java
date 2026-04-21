package com.herocounter.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.herocounter.app.ReminderUiHelper;
import android.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskSettingsActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executor;
    private SettingsTaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();

    // File picker launcher for CSV import
    private ActivityResultLauncher<String[]> csvPickerLauncher;

    // Document creator launcher for CSV export (local save)
    private ActivityResultLauncher<String> csvSaveLauncher;

    // Pending CSV content waiting to be written after user picks save location
    private String pendingCsvContent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_settings);

        db       = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Register file picker before activity is fully started
        csvPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) importCsv(uri);
                }
        );

        // Register document creator for local CSV save
        csvSaveLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/csv"),
                uri -> {
                    if (uri != null && pendingCsvContent != null) {
                        writeCsvToUri(uri, pendingCsvContent);
                        pendingCsvContent = null;
                    }
                }
        );

        TextView btnBack          = findViewById(R.id.btnBack);
        TextView btnAddCountInline = findViewById(R.id.btnAddCountInline);
        TextView btnExportBottom  = findViewById(R.id.btnExportCsvBottom);
        TextView btnImport        = findViewById(R.id.btnImportCsv);
        RecyclerView rv           = findViewById(R.id.rvSettingsTasks);

        adapter = new SettingsTaskAdapter(taskList, new SettingsTaskAdapter.Listener() {
            @Override public void onEdit(Task task)   { showEditDialog(task); }
            @Override public void onDelete(Task task) { showDeleteDialog(task); }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnAddCountInline.setOnClickListener(v -> showAddFromSettingsDialog());
        btnExportBottom.setOnClickListener(v -> showExportDialog());
        btnImport.setOnClickListener(v ->
                csvPickerLauncher.launch(new String[]{"text/csv", "text/comma-separated-values",
                        "application/csv", "text/plain", "*/*"}));

        // About section — expandable cards
        setupExpandable(
                findViewById(R.id.cardAppInfo),
                findViewById(R.id.expandAppInfo),
                findViewById(R.id.tvAppInfoChevron));
        setupExpandable(
                findViewById(R.id.cardVersionInfo),
                findViewById(R.id.expandVersionInfo),
                findViewById(R.id.tvVersionChevron));

        loadTasks();
    }

    private void loadTasks() {
        executor.execute(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                taskList.clear();
                taskList.addAll(tasks);
                adapter.setTasks(new ArrayList<>(taskList));
            });
        });
    }

    // ── Expandable card helper ────────────────────────────────────────────────

    private void setupExpandable(View card, View expandContent, TextView chevron) {
        card.setOnClickListener(v -> {
            boolean expanding = expandContent.getVisibility() == View.GONE;
            expandContent.setVisibility(expanding ? View.VISIBLE : View.GONE);
            chevron.setText(expanding ? "∨" : "›");
            chevron.setTextColor(expanding
                    ? getColor(R.color.accent_blue)
                    : getColor(R.color.text_muted));
        });
    }

        // ── Add Count dialog ──────────────────────────────────────────────────────

    private void showAddFromSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_count, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int maxH = (int)(getResources().getDisplayMetrics().heightPixels * 0.85f);
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.9f),
                maxH);

        EditText etName    = dialogView.findViewById(R.id.etTaskName);
        EditText etDaily   = dialogView.findViewById(R.id.etDailyGoal);
        EditText etWeekly  = dialogView.findViewById(R.id.etWeeklyGoal);
        EditText etMonthly = dialogView.findViewById(R.id.etMonthlyGoal);
        EditText etYearly  = dialogView.findViewById(R.id.etYearlyGoal);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        TextView btnSave   = dialogView.findViewById(R.id.btnDialogSave);

        ReminderUiHelper reminderUi = new ReminderUiHelper(this);
        reminderUi.bind(dialogView);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Required"); return; }
            Task task = new Task(name, System.currentTimeMillis(),
                    parseOptional(etDaily.getText().toString().trim()),
                    parseOptional(etWeekly.getText().toString().trim()),
                    parseOptional(etMonthly.getText().toString().trim()),
                    parseOptional(etYearly.getText().toString().trim()));
            reminderUi.applyTo(task);
            executor.execute(() -> {
                long id = db.taskDao().insert(task);
                task.id = (int) id;
                if (task.reminderEnabled) ReminderManager.scheduleReminder(this, task);
                loadTasks();
            });
            dialog.dismiss();
        });
        dialog.show();
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private void showEditDialog(Task task) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        // Inflate layout into a view first so we can bind reminder UI before show()
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_count, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Use 90% width, 85% max height so dialog scrolls if reminder options expand
        int maxH = (int)(getResources().getDisplayMetrics().heightPixels * 0.85f);
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.9f),
                maxH);

        EditText etName    = dialogView.findViewById(R.id.etTaskName);
        EditText etDaily   = dialogView.findViewById(R.id.etDailyGoal);
        EditText etWeekly  = dialogView.findViewById(R.id.etWeeklyGoal);
        EditText etMonthly = dialogView.findViewById(R.id.etMonthlyGoal);
        EditText etYearly  = dialogView.findViewById(R.id.etYearlyGoal);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        TextView btnSave   = dialogView.findViewById(R.id.btnDialogSave);

        etName.setText(task.name);
        etDaily.setText(task.dailyGoal > 0 ? String.valueOf(task.dailyGoal) : "");
        etWeekly.setText(task.weeklyGoal > 0 ? String.valueOf(task.weeklyGoal) : "");
        etMonthly.setText(task.monthlyGoal > 0 ? String.valueOf(task.monthlyGoal) : "");
        etYearly.setText(task.yearlyGoal > 0 ? String.valueOf(task.yearlyGoal) : "");

        ReminderUiHelper reminderUi = new ReminderUiHelper(this);
        reminderUi.bind(dialogView);
        reminderUi.populate(task);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Required"); return; }
            task.name        = name;
            task.dailyGoal   = parseOptional(etDaily.getText().toString().trim());
            task.weeklyGoal  = parseOptional(etWeekly.getText().toString().trim());
            task.monthlyGoal = parseOptional(etMonthly.getText().toString().trim());
            task.yearlyGoal  = parseOptional(etYearly.getText().toString().trim());
            reminderUi.applyTo(task);
            executor.execute(() -> {
                db.taskDao().update(task);
                if (task.reminderEnabled) ReminderManager.scheduleReminder(this, task);
                else ReminderManager.cancelReminder(this, task.id);
                loadTasks();
            });
            dialog.dismiss();
        });
        dialog.show();
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────

    private void showDeleteDialog(Task task) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvMsg     = dialog.findViewById(R.id.tvDeleteMessage);
        TextView btnCancel = dialog.findViewById(R.id.btnDialogCancel);
        TextView btnDelete = dialog.findViewById(R.id.btnDialogDelete);

        tvMsg.setText("Delete \"" + task.name + "\" and all its count history?\n\nThis cannot be undone.");
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            executor.execute(() -> {
                db.countEntryDao().deleteAllForTask(task.id);
                db.taskDao().delete(task);
                loadTasks();
            });
            dialog.dismiss();
        });
        dialog.show();
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void showExportDialog() {
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                if (allTasks.isEmpty()) {
                    Toast.makeText(this, "No counts to export", Toast.LENGTH_SHORT).show();
                    return;
                }

                Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                LinearLayout root = new LinearLayout(this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackground(getDrawable(R.drawable.rounded_surface));
                root.setPadding(24, 24, 24, 24);

                TextView title = new TextView(this);
                title.setText("Select Counts to Export");
                title.setTextColor(getColor(R.color.text_primary));
                title.setTextSize(17f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setPadding(0, 0, 0, 20);
                root.addView(title);

                List<CheckBox> checkBoxes = new ArrayList<>();
                for (Task t : allTasks) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(t.name);
                    cb.setTextColor(getColor(R.color.text_primary));
                    cb.setTextSize(14f);
                    cb.setButtonTintList(android.content.res.ColorStateList
                            .valueOf(getColor(R.color.accent_blue)));
                    cb.setChecked(true);
                    cb.setPadding(0, 8, 0, 8);
                    root.addView(cb);
                    checkBoxes.add(cb);
                }

                LinearLayout btns = new LinearLayout(this);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                blp.setMargins(0, 20, 0, 0);
                btns.setLayoutParams(blp);

                TextView btnCancel = new TextView(this);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, 44, 1f);
                clp.setMarginEnd(8);
                btnCancel.setLayoutParams(clp);
                btnCancel.setGravity(Gravity.CENTER);
                btnCancel.setText("CANCEL");
                btnCancel.setTextColor(getColor(R.color.text_secondary));
                btnCancel.setTextSize(13f);
                btnCancel.setBackground(getDrawable(R.drawable.btn_dialog_cancel));

                TextView btnExport = new TextView(this);
                btnExport.setLayoutParams(new LinearLayout.LayoutParams(0, 44, 1f));
                btnExport.setGravity(Gravity.CENTER);
                btnExport.setText("EXPORT");
                btnExport.setTextColor(getColor(R.color.text_primary));
                btnExport.setTextSize(13f);
                btnExport.setBackground(getDrawable(R.drawable.btn_dialog_save));

                btns.addView(btnCancel);
                btns.addView(btnExport);
                root.addView(btns);

                dialog.setContentView(root);
                dialog.getWindow().setLayout(
                        (int)(getResources().getDisplayMetrics().widthPixels * 0.85f),
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                btnCancel.setOnClickListener(v -> dialog.dismiss());
                btnExport.setOnClickListener(v -> {
                    List<Task> selected = new ArrayList<>();
                    for (int i = 0; i < checkBoxes.size(); i++) {
                        if (checkBoxes.get(i).isChecked()) selected.add(allTasks.get(i));
                    }
                    if (selected.isEmpty()) {
                        Toast.makeText(this, "Select at least one count", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    exportCsv(selected);
                });
                dialog.show();
            });
        });
    }

    private void exportCsv(List<Task> selected) {
        executor.execute(() -> {
            try {
                // Build CSV content in memory
                StringBuilder sb = new StringBuilder();
                sb.append("Task,Date,Week,Month,Year,Delta,Timestamp\n");
                for (Task t : selected) {
                    for (CountEntry e : db.countEntryDao().getAllEntriesForTask(t.id)) {
                        sb.append(esc(t.name)).append(",")
                          .append(e.dateDay).append(",")
                          .append(e.dateWeek).append(",")
                          .append(e.dateMonth).append(",")
                          .append(e.dateYear).append(",")
                          .append(String.valueOf(e.delta)).append(",")
                          .append(String.valueOf(e.timestamp)).append("\n");
                    }
                }
                final String csvContent = sb.toString();

                // Generate timestamped filename
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
                        .format(new Date());
                final String filename = "HeroCounter_export_" + timestamp + ".csv";

                runOnUiThread(() -> showExportMethodDialog(csvContent, filename));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Export failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Ask the user whether to save locally or share via another app.
     * Uses a custom dark-themed dialog matching the app's color scheme.
     */
    private void showExportMethodDialog(String csvContent, String filename) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(getDrawable(R.drawable.rounded_surface));
        root.setPadding(28, 28, 28, 28);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Export CSV");
        tvTitle.setTextColor(getColor(R.color.text_primary));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 10);
        root.addView(tvTitle);

        // Filename
        TextView tvFile = new TextView(this);
        tvFile.setText(filename);
        tvFile.setTextColor(getColor(R.color.accent_blue));
        tvFile.setTextSize(12f);
        tvFile.setPadding(0, 0, 0, 20);
        root.addView(tvFile);

        // Message
        TextView tvMsg = new TextView(this);
        tvMsg.setText("How would you like to export your data?");
        tvMsg.setTextColor(getColor(R.color.text_secondary));
        tvMsg.setTextSize(14f);
        tvMsg.setLineSpacing(0, 1.4f);
        tvMsg.setPadding(0, 0, 0, 24);
        root.addView(tvMsg);

        // Save to Device button
        TextView btnSave = new TextView(this);
        btnSave.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48));
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setText("⬇  Save to Device");
        btnSave.setTextColor(getColor(R.color.text_primary));
        btnSave.setTextSize(14f);
        btnSave.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSave.setBackground(getDrawable(R.drawable.btn_dialog_save));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48);
        saveLp.setMargins(0, 0, 0, 10);
        btnSave.setLayoutParams(saveLp);
        root.addView(btnSave);

        // Share / Send button
        TextView btnShare = new TextView(this);
        btnShare.setGravity(Gravity.CENTER);
        btnShare.setText("↗  Share / Send");
        btnShare.setTextColor(getColor(R.color.text_primary));
        btnShare.setTextSize(14f);
        btnShare.setTypeface(null, android.graphics.Typeface.BOLD);
        btnShare.setBackground(getDrawable(R.drawable.btn_dialog_save));
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48);
        shareLp.setMargins(0, 0, 0, 10);
        btnShare.setLayoutParams(shareLp);
        root.addView(btnShare);

        // Cancel button
        TextView btnCancel = new TextView(this);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setText("CANCEL");
        btnCancel.setTextColor(getColor(R.color.text_secondary));
        btnCancel.setTextSize(13f);
        btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
        btnCancel.setBackground(getDrawable(R.drawable.btn_dialog_cancel));
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48));
        root.addView(btnCancel);

        dialog.setContentView(root);
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            showPrivacyWarning(() -> {
                pendingCsvContent = csvContent;
                csvSaveLauncher.launch(filename);
            });
        });
        btnShare.setOnClickListener(v -> {
            dialog.dismiss();
            showPrivacyWarning(() -> shareCsv(csvContent, filename));
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Show a privacy warning before completing the export.
     * The user must tap "I Understand" to proceed.
     * onConfirmed runs only after confirmation.
     */
    private void showPrivacyWarning(Runnable onConfirmed) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false); // must tap a button

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(getDrawable(R.drawable.rounded_surface));
        root.setPadding(28, 28, 28, 28);

        // Warning title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚠  WARNING!!!");
        tvTitle.setTextColor(getColor(R.color.btn_minus));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 18);
        root.addView(tvTitle);

        // Warning message
        TextView tvMsg = new TextView(this);
        tvMsg.setText("The CSV you are about to create is unencrypted!\n\nIf the file contains sensitive data, it is recommended that you store the file safely in an encrypted container to maintain your privacy.");
        tvMsg.setTextColor(getColor(R.color.text_secondary));
        tvMsg.setTextSize(14f);
        tvMsg.setLineSpacing(0, 1.5f);
        tvMsg.setPadding(0, 0, 0, 28);
        root.addView(tvMsg);

        // Buttons row
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCancel = new TextView(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, 48, 1f);
        clp.setMarginEnd(10);
        btnCancel.setLayoutParams(clp);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setText("CANCEL");
        btnCancel.setTextColor(getColor(R.color.text_secondary));
        btnCancel.setTextSize(13f);
        btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
        btnCancel.setBackground(getDrawable(R.drawable.btn_dialog_cancel));

        TextView btnUnderstand = new TextView(this);
        btnUnderstand.setLayoutParams(new LinearLayout.LayoutParams(0, 48, 1f));
        btnUnderstand.setGravity(Gravity.CENTER);
        btnUnderstand.setText("I Understand");
        btnUnderstand.setTextColor(getColor(R.color.text_primary));
        btnUnderstand.setTextSize(13f);
        btnUnderstand.setTypeface(null, android.graphics.Typeface.BOLD);
        btnUnderstand.setBackground(getDrawable(R.drawable.btn_dialog_save));

        btns.addView(btnCancel);
        btns.addView(btnUnderstand);
        root.addView(btns);

        dialog.setContentView(root);
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnUnderstand.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirmed.run();
        });

        dialog.show();
    }

    /**
     * Write CSV content to a URI chosen by the user via the system file picker.
     */
    private void writeCsvToUri(android.net.Uri uri, String csvContent) {
        executor.execute(() -> {
            try {
                java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                if (os == null) throw new IOException("Could not open output stream");
                os.write(csvContent.getBytes("UTF-8"));
                os.flush();
                os.close();
                runOnUiThread(() ->
                        Toast.makeText(this, "CSV saved successfully!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Share CSV via email, messaging, cloud, etc.
     */
    private void shareCsv(String csvContent, String filename) {
        executor.execute(() -> {
            try {
                File dir = new File(getCacheDir(), "exports");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, filename);
                FileWriter w = new FileWriter(f);
                w.write(csvContent);
                w.flush();
                w.close();

                Uri uri = FileProvider.getUriForFile(this,
                        "com.herocounter.app.fileprovider", f);
                Intent si = new Intent(Intent.ACTION_SEND);
                si.setType("text/csv");
                si.putExtra(Intent.EXTRA_STREAM, uri);
                si.putExtra(Intent.EXTRA_SUBJECT, "Hero Counter Data Export — " + filename);
                si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() ->
                        startActivity(Intent.createChooser(si, "Share CSV via…")));
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Share failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    // ── CSV Import ────────────────────────────────────────────────────────────

    private void importCsv(Uri uri) {
        // Show progress toast
        runOnUiThread(() -> Toast.makeText(this,
                "Reading CSV…", Toast.LENGTH_SHORT).show());

        executor.execute(() -> {
            int importedEntries = 0;
            int skippedDupes    = 0;
            int createdTasks    = 0;
            String errorMsg     = null;

            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new IOException("Could not open file");

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                // Build a name→task map from existing tasks
                Map<String, Task> taskMap = new HashMap<>();
                for (Task t : db.taskDao().getAllTasks()) {
                    taskMap.put(t.name.trim().toLowerCase(), t);
                }

                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Skip header row
                    if (firstLine) {
                        firstLine = false;
                        if (line.startsWith("Task,")) continue;
                    }

                    if (line.trim().isEmpty()) continue;

                    // Parse CSV row: Task,Date,Week,Month,Year,Delta,Timestamp
                    String[] cols = parseCsvLine(line);
                    if (cols.length < 7) continue;

                    String taskName  = cols[0].trim();
                    String dateDay   = cols[1].trim();
                    String dateWeek  = cols[2].trim();
                    String dateMonth = cols[3].trim();
                    String dateYear  = cols[4].trim();
                    int delta;
                    long timestamp;

                    try {
                        delta     = Integer.parseInt(cols[5].trim());
                        timestamp = Long.parseLong(cols[6].trim());
                    } catch (NumberFormatException e) {
                        continue; // skip malformed rows
                    }

                    if (taskName.isEmpty()) continue;

                    // Find or create the task
                    String key = taskName.toLowerCase();
                    if (!taskMap.containsKey(key)) {
                        Task newTask = new Task(taskName, System.currentTimeMillis(),
                                0, 0, 0, 0);
                        long newId = db.taskDao().insert(newTask);
                        newTask.id = (int) newId;
                        taskMap.put(key, newTask);
                        createdTasks++;
                    }
                    Task task = taskMap.get(key);

                    // Skip duplicate entries (same task + timestamp + delta)
                    int dupeCount = db.countEntryDao()
                            .countDuplicates(task.id, timestamp, delta);
                    if (dupeCount > 0) {
                        skippedDupes++;
                        continue;
                    }

                    // Insert the entry
                    CountEntry entry = new CountEntry(
                            task.id, delta, timestamp,
                            dateDay, dateWeek, dateMonth, dateYear);
                    db.countEntryDao().insert(entry);
                    importedEntries++;
                }

                reader.close();
                is.close();

            } catch (Exception e) {
                errorMsg = e.getMessage();
            }

            final int fEntries  = importedEntries;
            final int fDupes    = skippedDupes;
            final int fTasks    = createdTasks;
            final String fError = errorMsg;

            runOnUiThread(() -> {
                if (fError != null) {
                    showResultDialog("Import Failed",
                            "Could not read the CSV file:\n\n" + fError,
                            false);
                    return;
                }

                String msg = fEntries + " entr" + (fEntries == 1 ? "y" : "ies") + " imported";
                if (fTasks > 0)
                    msg += "\n" + fTasks + " new count" + (fTasks == 1 ? "" : "s") + " created";
                if (fDupes > 0)
                    msg += "\n" + fDupes + " duplicate" + (fDupes == 1 ? "" : "s") + " skipped";

                showResultDialog("Import Complete", msg, true);
                loadTasks();
            });
        });
    }

    /**
     * Parse a single CSV line respecting quoted fields (e.g. task names with commas).
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private void showResultDialog(String title, String message, boolean success) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(getDrawable(R.drawable.rounded_surface));
        root.setPadding(28, 28, 28, 28);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(success ? R.color.accent_blue : R.color.btn_minus));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(getColor(R.color.text_secondary));
        tvMsg.setTextSize(14f);
        tvMsg.setLineSpacing(0, 1.4f);
        tvMsg.setPadding(0, 0, 0, 24);
        root.addView(tvMsg);

        TextView btnOk = new TextView(this);
        btnOk.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 44));
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setText("OK");
        btnOk.setTextColor(getColor(R.color.text_primary));
        btnOk.setTextSize(13f);
        btnOk.setTypeface(null, android.graphics.Typeface.BOLD);
        btnOk.setBackground(getDrawable(R.drawable.btn_dialog_save));
        root.addView(btnOk);

        dialog.setContentView(root);
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.82f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String esc(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private int parseOptional(String s) {
        if (s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
