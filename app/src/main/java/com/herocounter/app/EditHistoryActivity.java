package com.herocounter.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditHistoryActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executor;

    private List<Task> taskList = new ArrayList<>();
    private Task selectedTask = null;
    private Calendar selectedCal = Calendar.getInstance();

    private TextView tvSelectedDate, tvDayOfWeek, tvCurrentTotal;
    private LinearLayout taskChipGroup;
    private EditText etExactTotal;

    private final SimpleDateFormat displayFmt = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final SimpleDateFormat dowFmt     = new SimpleDateFormat("EEEE", Locale.US);
    private final SimpleDateFormat dayKeyFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_history);

        db       = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Default to yesterday
        selectedCal.add(Calendar.DAY_OF_YEAR, -1);

        bindViews();
        loadTasks();
    }

    private void bindViews() {
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        tvDayOfWeek     = findViewById(R.id.tvDayOfWeek);
        tvCurrentTotal  = findViewById(R.id.tvCurrentDayTotal);
        taskChipGroup   = findViewById(R.id.taskChipGroup);
        etExactTotal    = findViewById(R.id.etExactTotal);

        // Date nav
        findViewById(R.id.btnPrevDay).setOnClickListener(v -> shiftDay(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> shiftDay(1));
        findViewById(R.id.btnPickDate).setOnClickListener(v -> openDatePicker());

        // Adjust buttons
        findViewById(R.id.btnDecrement).setOnClickListener(v -> adjustBy(-1));
        findViewById(R.id.btnIncrement).setOnClickListener(v -> adjustBy(1));

        // Set exact
        findViewById(R.id.btnSetExact).setOnClickListener(v -> {
            String val = etExactTotal.getText().toString().trim();
            if (val.isEmpty()) { etExactTotal.setError("Enter a value"); return; }
            setExactTotal(Integer.parseInt(val));
        });

        refreshDateDisplay();
    }

    private void loadTasks() {
        executor.execute(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                taskList.clear();
                taskList.addAll(tasks);
                buildTaskChips();
                if (!taskList.isEmpty()) {
                    // Pre-select task passed via intent, or first task
                    int preselect = getIntent().getIntExtra("taskId", -1);
                    selectedTask = taskList.get(0);
                    for (Task t : taskList) {
                        if (t.id == preselect) { selectedTask = t; break; }
                    }
                    highlightChip(selectedTask.id);
                    refreshTotal();
                }
            });
        });
    }

    private void buildTaskChips() {
        taskChipGroup.removeAllViews();
        for (Task t : taskList) {
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setText(t.name);
            chip.setTextSize(13f);
            chip.setPadding(24, 12, 24, 12);
            chip.setId(t.id);
            chip.setBackground(getDrawable(R.drawable.tab_unselected));
            chip.setTextColor(getColor(R.color.text_secondary));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                selectedTask = t;
                highlightChip(t.id);
                refreshTotal();
            });
            taskChipGroup.addView(chip);
        }
    }

    private void highlightChip(int taskId) {
        for (int i = 0; i < taskChipGroup.getChildCount(); i++) {
            TextView chip = (TextView) taskChipGroup.getChildAt(i);
            boolean active = (chip.getId() == taskId);
            chip.setBackground(getDrawable(active
                    ? R.drawable.tab_selected : R.drawable.tab_unselected));
            chip.setTextColor(getColor(active
                    ? R.color.text_primary : R.color.text_secondary));
        }
    }

    private void shiftDay(int delta) {
        selectedCal.add(Calendar.DAY_OF_YEAR, delta);
        refreshDateDisplay();
        refreshTotal();
    }

    private void openDatePicker() {
        int y = selectedCal.get(Calendar.YEAR);
        int m = selectedCal.get(Calendar.MONTH);
        int d = selectedCal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedCal.set(year, month, day);
                    refreshDateDisplay();
                    refreshTotal();
                }, y, m, d);

        // Allow any past date — set max to today
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        // No minimum — allow dates before app install
        picker.show();
    }

    private void refreshDateDisplay() {
        tvSelectedDate.setText(displayFmt.format(selectedCal.getTime()));
        tvDayOfWeek.setText(dowFmt.format(selectedCal.getTime()));
    }

    private void refreshTotal() {
        if (selectedTask == null) return;
        String dayKey = dayKeyFmt.format(selectedCal.getTime());
        executor.execute(() -> {
            int total = db.countEntryDao().getTotalForDay(selectedTask.id, dayKey);
            runOnUiThread(() -> tvCurrentTotal.setText(String.valueOf(total)));
        });
    }

    private void adjustBy(int delta) {
        if (selectedTask == null) {
            Toast.makeText(this, "Select a count first", Toast.LENGTH_SHORT).show();
            return;
        }
        long ts = selectedCal.getTimeInMillis();
        String dayKey = dayKeyFmt.format(selectedCal.getTime());

        CountEntry entry = new CountEntry(
                selectedTask.id, delta, ts,
                dayKey,
                DateUtils.weekForTimestamp(ts),
                DateUtils.monthForTimestamp(ts),
                DateUtils.yearForTimestamp(ts)
        );
        executor.execute(() -> {
            db.countEntryDao().insert(entry);
            int newTotal = db.countEntryDao().getTotalForDay(selectedTask.id, dayKey);
            runOnUiThread(() -> {
                tvCurrentTotal.setText(String.valueOf(newTotal));
                Toast.makeText(this,
                        (delta > 0 ? "+" : "") + delta + " → " + newTotal,
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void setExactTotal(int target) {
        if (selectedTask == null) {
            Toast.makeText(this, "Select a count first", Toast.LENGTH_SHORT).show();
            return;
        }
        String dayKey = dayKeyFmt.format(selectedCal.getTime());
        long ts = selectedCal.getTimeInMillis();

        executor.execute(() -> {
            int current = db.countEntryDao().getTotalForDay(selectedTask.id, dayKey);
            int delta = target - current;
            if (delta == 0) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Already at " + target, Toast.LENGTH_SHORT).show());
                return;
            }
            CountEntry corrective = new CountEntry(
                    selectedTask.id, delta, ts,
                    dayKey,
                    DateUtils.weekForTimestamp(ts),
                    DateUtils.monthForTimestamp(ts),
                    DateUtils.yearForTimestamp(ts)
            );
            db.countEntryDao().insert(corrective);
            runOnUiThread(() -> {
                tvCurrentTotal.setText(String.valueOf(target));
                etExactTotal.setText("");
                Toast.makeText(this,
                        "Set to " + target + " (delta: " + (delta >= 0 ? "+" : "") + delta + ")",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
