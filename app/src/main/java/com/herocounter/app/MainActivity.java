package com.herocounter.app;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executor;

    private TaskAdapter taskAdapter;
    private List<Task> taskList = new ArrayList<>();

    private Task selectedTask = null;
    private boolean addMode = true;

    private int lastGoalFiredTaskId = -1;
    private String lastGoalFiredDay = "";

    private TextView tvCount, tvTaskName, tvMode;
    private TextView tvToday, tvYesterday, tvWeekly;
    private TextView btnMinus, btnPlus, btnSettings, btnViewStats, btnNewCount;
    private TextView btnEditHistory;
    private FrameLayout counterButton;
    private RecyclerView rvTasks;
    private FrameLayout rootLayout;

    private BroadcastReceiver midnightReceiver;

    // Notification permission launcher (Android 13+)
    private ActivityResultLauncher<String> notifPermLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Register notification permission launcher
        notifPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted)
                        Toast.makeText(this,
                                "Notifications blocked — reminders won't fire",
                                Toast.LENGTH_LONG).show();
                });

        bindViews();
        setupTaskList();
        setupListeners();

        // Handle tap from a reminder notification — pre-select the task
        int notifTaskId = getIntent().getIntExtra(ReminderReceiver.EXTRA_TASK_ID, -1);
        if (notifTaskId >= 0) {
            executor.execute(() -> {
                Task t = db.taskDao().getTaskById(notifTaskId);
                if (t != null) runOnUiThread(() -> selectTask(t));
            });
        }

        loadTasks();

        MidnightResetReceiver.scheduleMidnightAlarm(this);

        midnightReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent i) {
                if (MidnightResetReceiver.ACTION_MIDNIGHT_RESET.equals(i.getAction()))
                    onMidnightReset();
            }
        };
        registerReceiver(midnightReceiver,
                new IntentFilter(MidnightResetReceiver.ACTION_MIDNIGHT_RESET),
                Context.RECEIVER_NOT_EXPORTED);
    }

    private void onMidnightReset() {
        runOnUiThread(() -> { tvCount.setText("0"); tvToday.setText("0"); refreshStats(); });
    }

    private void bindViews() {
        tvCount       = findViewById(R.id.tvCount);
        tvTaskName    = findViewById(R.id.tvTaskName);
        tvMode        = findViewById(R.id.tvMode);
        tvToday       = findViewById(R.id.tvToday);
        tvYesterday   = findViewById(R.id.tvYesterday);
        tvWeekly      = findViewById(R.id.tvWeekly);
        btnMinus      = findViewById(R.id.btnMinus);
        btnPlus       = findViewById(R.id.btnPlus);
        btnSettings   = findViewById(R.id.btnSettings);
        btnViewStats  = findViewById(R.id.btnViewStats);
        btnNewCount   = findViewById(R.id.btnNewCount);
        btnEditHistory= findViewById(R.id.btnEditHistory);
        counterButton = findViewById(R.id.counterButton);
        rvTasks       = findViewById(R.id.rvTasks);
        rootLayout    = (FrameLayout) getWindow().getDecorView()
                            .findViewById(android.R.id.content);
    }

    private void setupTaskList() {
        taskAdapter = new TaskAdapter(taskList, task -> selectTask(task));
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void selectTask(Task task) {
        selectedTask = task;
        taskAdapter.setSelectedTaskId(task.id);
        tvTaskName.setText(task.name);
        refreshStats();
    }

    private void setupListeners() {
        counterButton.setOnClickListener(v -> {
            if (selectedTask == null) {
                Toast.makeText(this, "Select a count first", Toast.LENGTH_SHORT).show();
                return;
            }
            counterButton.startAnimation(
                    AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            recordCount(addMode ? 1 : -1);
        });

        btnMinus.setOnClickListener(v -> {
            addMode = false;
            tvMode.setText("− SUB");
            tvMode.setTextColor(getColor(R.color.btn_minus));
        });

        btnPlus.setOnClickListener(v -> {
            addMode = true;
            tvMode.setText("+ ADD");
            tvMode.setTextColor(getColor(R.color.accent_blue));
        });

        btnNewCount.setOnClickListener(v -> showNewCountDialog());

        btnEditHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditHistoryActivity.class);
            if (selectedTask != null) intent.putExtra("taskId", selectedTask.id);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, TaskSettingsActivity.class)));

        btnViewStats.setOnClickListener(v -> {
            if (selectedTask == null) {
                Toast.makeText(this, "Select a count first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, StatisticsActivity.class);
            intent.putExtra("taskId", selectedTask.id);
            intent.putExtra("taskName", selectedTask.name);
            startActivity(intent);
        });
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    private void recordCount(int delta) {
        long now = System.currentTimeMillis();
        CountEntry entry = new CountEntry(
                selectedTask.id, delta, now,
                DateUtils.dayForTimestamp(now),
                DateUtils.weekForTimestamp(now),
                DateUtils.monthForTimestamp(now),
                DateUtils.yearForTimestamp(now));
        executor.execute(() -> {
            db.countEntryDao().insert(entry);
            int todayTotal     = db.countEntryDao().getTotalForDay(selectedTask.id, DateUtils.today());
            int yesterdayTotal = db.countEntryDao().getTotalForDay(selectedTask.id, DateUtils.yesterday());
            int weeklyTotal    = db.countEntryDao().getTotalForWeek(selectedTask.id, DateUtils.currentWeek());
            int dailyGoal      = selectedTask.dailyGoal;
            runOnUiThread(() -> {
                tvCount.setText(String.valueOf(todayTotal));
                tvToday.setText(String.valueOf(todayTotal));
                tvYesterday.setText(String.valueOf(yesterdayTotal));
                tvWeekly.setText(String.valueOf(weeklyTotal));
                String today = DateUtils.today();
                boolean alreadyFired = (lastGoalFiredTaskId == selectedTask.id
                        && lastGoalFiredDay.equals(today));
                if (dailyGoal > 0 && todayTotal >= dailyGoal && !alreadyFired) {
                    lastGoalFiredTaskId = selectedTask.id;
                    lastGoalFiredDay    = today;
                    launchFireworks();
                }
            });
        });
    }

    // ── Fireworks ─────────────────────────────────────────────────────────────

    private void launchFireworks() {
        FireworksView fw = new FireworksView(this);
        fw.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fw.setBackgroundColor(Color.argb(140, 0, 0, 0));
        fw.setClickable(false);
        rootLayout.addView(fw);
        fw.start(() -> rootLayout.post(() -> rootLayout.removeView(fw)));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void refreshStats() {
        if (selectedTask == null) return;
        int taskId = selectedTask.id;
        executor.execute(() -> {
            int t = db.countEntryDao().getTotalForDay(taskId, DateUtils.today());
            int y = db.countEntryDao().getTotalForDay(taskId, DateUtils.yesterday());
            int w = db.countEntryDao().getTotalForWeek(taskId, DateUtils.currentWeek());
            runOnUiThread(() -> {
                tvCount.setText(String.valueOf(t));
                tvToday.setText(String.valueOf(t));
                tvYesterday.setText(String.valueOf(y));
                tvWeekly.setText(String.valueOf(w));
            });
        });
    }

    // ── New Count dialog ──────────────────────────────────────────────────────

    private void showNewCountDialog() {
        // Request notification permission if needed before showing dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Inflate into a View first so reminder widgets are accessible before show()
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_count, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int maxH = (int)(getResources().getDisplayMetrics().heightPixels * 0.85f);
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.92f),
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
                int maxOrder = db.taskDao().getMaxSortOrder();
                task.sortOrder = maxOrder + 1;
                long id = db.taskDao().insert(task);
                task.id = (int) id;
                if (task.reminderEnabled) ReminderManager.scheduleReminder(this, task);
                loadTasks();
            });
            dialog.dismiss();
        });
        dialog.show();
    }

    // ── Task loading ──────────────────────────────────────────────────────────

    private void loadTasks() {
        executor.execute(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                taskList.clear();
                taskList.addAll(tasks);
                taskAdapter.setTasks(new ArrayList<>(taskList));
                if (!taskList.isEmpty() && selectedTask == null) {
                    selectTask(taskList.get(0));
                } else if (selectedTask != null) {
                    for (Task t : taskList) {
                        if (t.id == selectedTask.id) { selectedTask = t; tvTaskName.setText(t.name); break; }
                    }
                    refreshStats();
                }
            });
        });
    }

    private int parseOptional(String s) {
        if (s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    protected void onResume() { super.onResume(); loadTasks(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (midnightReceiver != null) unregisterReceiver(midnightReceiver);
        executor.shutdown();
    }
}
