package com.herocounter.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executor;

    private int taskId;
    private String taskName;
    private Task currentTask;

    private TextView tabDaily, tabWeekly, tabMonthly, tabYearly;
    private String currentPeriod = "daily";

    private BarChart barChart;
    private TextView tvSummaryTotal, tvAvgValue, tvStatsTitle;
    private LinearLayout tableContainer, projectionContainer;

    // For history editing — tracks the date being edited
    private Calendar editCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        db       = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        taskId   = getIntent().getIntExtra("taskId", -1);
        taskName = getIntent().getStringExtra("taskName");

        bindViews();
        setupTabs();
        setupToolbar();
        loadTaskThenData();
    }

    private void bindViews() {
        tvStatsTitle       = findViewById(R.id.tvStatsTitle);
        barChart           = findViewById(R.id.barChart);
        tvSummaryTotal     = findViewById(R.id.tvSummaryTotal);
        tvAvgValue         = findViewById(R.id.tvAvgValue);
        tableContainer     = findViewById(R.id.tableContainer);
        projectionContainer= findViewById(R.id.projectionContainer);
        tabDaily           = findViewById(R.id.tabDaily);
        tabWeekly          = findViewById(R.id.tabWeekly);
        tabMonthly         = findViewById(R.id.tabMonthly);
        tabYearly          = findViewById(R.id.tabYearly);
        tvStatsTitle.setText(taskName);
    }

    private void setupToolbar() {
        TextView btnBack      = findViewById(R.id.btnBack);
        TextView btnEditHist  = findViewById(R.id.btnEditHistory);
        btnBack.setOnClickListener(v -> finish());
        btnEditHist.setOnClickListener(v -> showEditHistoryDialog());
    }

    private void setupTabs() {
        tabDaily.setOnClickListener(v   -> selectTab("daily"));
        tabWeekly.setOnClickListener(v  -> selectTab("weekly"));
        tabMonthly.setOnClickListener(v -> selectTab("monthly"));
        tabYearly.setOnClickListener(v  -> selectTab("yearly"));
        styleChart();
    }

    private void selectTab(String period) {
        currentPeriod = period;
        int unsel = getColor(R.color.text_secondary);
        int sel   = getColor(R.color.text_primary);
        tabDaily.setBackgroundResource(R.drawable.tab_unselected);   tabDaily.setTextColor(unsel);
        tabWeekly.setBackgroundResource(R.drawable.tab_unselected);  tabWeekly.setTextColor(unsel);
        tabMonthly.setBackgroundResource(R.drawable.tab_unselected); tabMonthly.setTextColor(unsel);
        tabYearly.setBackgroundResource(R.drawable.tab_unselected);  tabYearly.setTextColor(unsel);
        switch (period) {
            case "daily":   tabDaily.setBackgroundResource(R.drawable.tab_selected);   tabDaily.setTextColor(sel);   break;
            case "weekly":  tabWeekly.setBackgroundResource(R.drawable.tab_selected);  tabWeekly.setTextColor(sel);  break;
            case "monthly": tabMonthly.setBackgroundResource(R.drawable.tab_selected); tabMonthly.setTextColor(sel); break;
            case "yearly":  tabYearly.setBackgroundResource(R.drawable.tab_selected);  tabYearly.setTextColor(sel);  break;
        }
        loadData();
    }

    private void styleChart() {
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setNoDataText("No data yet");
        barChart.setNoDataTextColor(Color.parseColor("#6B7585"));
        XAxis x = barChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setTextColor(Color.parseColor("#B0B8C8"));
        x.setTextSize(10f);
        x.setGranularity(1f);
        YAxis y = barChart.getAxisLeft();
        y.setDrawGridLines(true);
        y.setGridColor(Color.parseColor("#333A47"));
        y.setTextColor(Color.parseColor("#B0B8C8"));
        y.setTextSize(10f);
        y.setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
        barChart.setBackgroundColor(Color.TRANSPARENT);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
    }

    private void loadTaskThenData() {
        executor.execute(() -> {
            currentTask = db.taskDao().getTaskById(taskId);
            loadData();
        });
    }

    private void loadData() {
        executor.execute(() -> {
            List<PeriodTotal> data;
            switch (currentPeriod) {
                case "weekly":  data = db.countEntryDao().getWeeklyTotals(taskId);  break;
                case "monthly": data = db.countEntryDao().getMonthlyTotals(taskId); break;
                case "yearly":  data = db.countEntryDao().getYearlyTotals(taskId);  break;
                default:        data = db.countEntryDao().getDailyTotals(taskId);   break;
            }
            List<PeriodTotal> allDaily = db.countEntryDao().getDailyTotals(taskId);
            final List<PeriodTotal> fd     = data;
            final List<PeriodTotal> fDaily = allDaily;
            runOnUiThread(() -> { updateChart(fd); updateProjections(fDaily); });
        });
    }

    private void updateChart(List<PeriodTotal> data) {
        if (data == null || data.isEmpty()) {
            barChart.clear(); barChart.invalidate();
            tvSummaryTotal.setText("—"); tvAvgValue.setText("—");
            tableContainer.removeAllViews();
            TextView e = new TextView(this);
            e.setText("No data yet for this period.");
            e.setTextColor(getColor(R.color.text_muted));
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, 32, 0, 32);
            tableContainer.addView(e);
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < data.size(); i++) {
            PeriodTotal pt = data.get(i);
            entries.add(new BarEntry(i, pt.total));
            labels.add(shortLabel(pt.period));
            total += pt.total;
        }
        BarDataSet ds = new BarDataSet(entries, taskName);
        ds.setColor(Color.parseColor("#4A90D9"));
        ds.setValueTextColor(Color.parseColor("#B0B8C8"));
        ds.setValueTextSize(9f);
        BarData bd = new BarData(ds);
        bd.setBarWidth(0.6f);
        barChart.setData(bd);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(Math.min(labels.size(), 7));
        barChart.animateY(400);
        barChart.invalidate();
        int avg = data.size() > 0 ? total / data.size() : 0;
        tvSummaryTotal.setText(String.valueOf(total));
        tvAvgValue.setText(String.valueOf(avg));
        tableContainer.removeAllViews();
        tableContainer.addView(makeRow("Period", "Count", true));
        for (PeriodTotal pt : data) {
            tableContainer.addView(makeDivider());
            tableContainer.addView(makeRow(pt.period, String.valueOf(pt.total), false));
        }
    }

    // ── Edit History ─────────────────────────────────────────────────────────

    private void showEditHistoryDialog() {
        editCalendar = Calendar.getInstance();
        editCalendar.add(Calendar.DAY_OF_YEAR, -1); // Start on yesterday

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_history);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.9f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvTask        = dialog.findViewById(R.id.tvEditHistoryTask);
        TextView tvDate        = dialog.findViewById(R.id.tvSelectedDate);
        TextView tvCurrentTotal= dialog.findViewById(R.id.tvCurrentDayTotal);
        TextView btnPrev       = dialog.findViewById(R.id.btnPrevDay);
        TextView btnNext       = dialog.findViewById(R.id.btnNextDay);
        EditText etNewTotal    = dialog.findViewById(R.id.etNewTotal);
        TextView btnCancel     = dialog.findViewById(R.id.btnDialogCancel);
        TextView btnSave       = dialog.findViewById(R.id.btnDialogSave);

        tvTask.setText(taskName);

        SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, MMM d yyyy", Locale.US);
        SimpleDateFormat dayFmt     = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Runnable refreshDate = () -> {
            tvDate.setText(displayFmt.format(editCalendar.getTime()));
            String dayKey = dayFmt.format(editCalendar.getTime());
            executor.execute(() -> {
                int current = db.countEntryDao().getTotalForDay(taskId, dayKey);
                runOnUiThread(() -> tvCurrentTotal.setText(String.valueOf(current)));
            });
        };
        refreshDate.run();

        btnPrev.setOnClickListener(v -> {
            editCalendar.add(Calendar.DAY_OF_YEAR, -1);
            etNewTotal.setText("");
            refreshDate.run();
        });

        btnNext.setOnClickListener(v -> {
            // Don't allow future dates
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            if (editCalendar.before(tomorrow)) {
                editCalendar.add(Calendar.DAY_OF_YEAR, 1);
                etNewTotal.setText("");
                refreshDate.run();
            } else {
                Toast.makeText(this, "Cannot edit future dates", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTotalStr = etNewTotal.getText().toString().trim();
            if (newTotalStr.isEmpty()) {
                etNewTotal.setError("Enter a value");
                return;
            }
            int newTotal = Integer.parseInt(newTotalStr);
            String dayKey = dayFmt.format(editCalendar.getTime());
            long ts = editCalendar.getTimeInMillis();

            executor.execute(() -> {
                int currentTotal = db.countEntryDao().getTotalForDay(taskId, dayKey);
                int delta = newTotal - currentTotal;
                if (delta == 0) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "No change needed — total is already " + newTotal,
                            Toast.LENGTH_SHORT).show());
                    return;
                }
                // Insert a corrective entry
                CountEntry corrective = new CountEntry(
                        taskId, delta, ts,
                        dayFmt.format(editCalendar.getTime()),
                        DateUtils.weekForTimestamp(ts),
                        DateUtils.monthForTimestamp(ts),
                        DateUtils.yearForTimestamp(ts)
                );
                db.countEntryDao().insert(corrective);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Entry updated: " + dayKey + " → " + newTotal,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadData(); // refresh chart
                });
            });
        });

        dialog.show();
    }

    // ── Projections ───────────────────────────────────────────────────────────

    private void updateProjections(List<PeriodTotal> allDaily) {
        projectionContainer.removeAllViews();
        if (currentTask == null || allDaily == null || allDaily.isEmpty()) {
            projectionContainer.setVisibility(View.GONE);
            return;
        }
        projectionContainer.setVisibility(View.VISIBLE);

        TextView header = new TextView(this);
        header.setText("PROJECTIONS & GOALS");
        header.setTextColor(getColor(R.color.text_secondary));
        header.setTextSize(11f);
        header.setLetterSpacing(0.1f);
        header.setPadding(0, 0, 0, 12);
        projectionContainer.addView(header);

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min  = cal.get(Calendar.MINUTE);
        double dayFraction = (hour * 60.0 + min) / (24.0 * 60.0);

        // Daily
        String today = DateUtils.today();
        int todayCount = 0;
        for (PeriodTotal pt : allDaily) if (pt.period.equals(today)) { todayCount = pt.total; break; }
        int projDay = dayFraction > 0.01 ? (int) Math.round(todayCount / dayFraction) : todayCount;
        addProjectionRow("Today projected finish", projDay, currentTask.dailyGoal, "Daily goal: ");

        // Weekly
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int daysElapsed = (dow == Calendar.SUNDAY) ? 7 : dow - 1;
        double weekFraction = (daysElapsed - 1 + dayFraction) / 7.0;
        int weekCount = 0;
        for (PeriodTotal pt : allDaily) if (isInCurrentWeek(pt.period)) weekCount += pt.total;
        int projWeek = weekFraction > 0.01 ? (int) Math.round(weekCount / weekFraction) : weekCount;
        addProjectionRow("This week projected", projWeek, currentTask.weeklyGoal, "Weekly goal: ");

        // Monthly
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfMonth  = cal.get(Calendar.DAY_OF_MONTH);
        double monthFraction = (dayOfMonth - 1 + dayFraction) / daysInMonth;
        String thisMonth = DateUtils.currentMonth();
        int monthCount = 0;
        for (PeriodTotal pt : allDaily) if (pt.period.startsWith(thisMonth)) monthCount += pt.total;
        int projMonth = monthFraction > 0.01 ? (int) Math.round(monthCount / monthFraction) : monthCount;
        addProjectionRow("This month projected", projMonth, currentTask.monthlyGoal, "Monthly goal: ");

        // Yearly
        int daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        int dayOfYear  = cal.get(Calendar.DAY_OF_YEAR);
        double yearFraction = (dayOfYear - 1 + dayFraction) / daysInYear;
        String thisYear = DateUtils.currentYear();
        int yearCount = 0;
        for (PeriodTotal pt : allDaily) if (pt.period.startsWith(thisYear)) yearCount += pt.total;
        int projYear = yearFraction > 0.01 ? (int) Math.round(yearCount / yearFraction) : yearCount;
        addProjectionRow("This year projected", projYear, currentTask.yearlyGoal, "Yearly goal: ");
    }

    private boolean isInCurrentWeek(String dateDay) {
        try {
            String[] p = dateDay.split("-");
            Calendar c = Calendar.getInstance(Locale.US);
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            c.setMinimalDaysInFirstWeek(4);
            int w = c.get(Calendar.WEEK_OF_YEAR), y = c.get(Calendar.YEAR);
            return String.format(Locale.US, "%04d-W%02d", y, w).equals(DateUtils.currentWeek());
        } catch (Exception e) { return false; }
    }

    private void addProjectionRow(String label, int projected, int goal, String goalLabel) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(getDrawable(R.drawable.rounded_surface));
        row.setPadding(16, 14, 16, 14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        row.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvL = new TextView(this);
        tvL.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvL.setText(label);
        tvL.setTextColor(getColor(R.color.text_secondary));
        tvL.setTextSize(13f);

        TextView tvP = new TextView(this);
        tvP.setText(String.valueOf(projected));
        tvP.setTextColor(getColor(R.color.accent_blue));
        tvP.setTextSize(20f);
        tvP.setTypeface(null, android.graphics.Typeface.BOLD);

        top.addView(tvL); top.addView(tvP);
        row.addView(top);

        if (goal > 0) {
            LinearLayout bot = new LinearLayout(this);
            bot.setOrientation(LinearLayout.HORIZONTAL);
            bot.setGravity(Gravity.CENTER_VERTICAL);
            bot.setPadding(0, 6, 0, 0);

            TextView tvG = new TextView(this);
            tvG.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvG.setText(goalLabel + goal);
            tvG.setTextColor(getColor(R.color.text_muted));
            tvG.setTextSize(12f);

            boolean onTrack = projected >= goal;
            TextView tvS = new TextView(this);
            tvS.setText(onTrack ? "✅ On track" : "🔴 Behind pace");
            tvS.setTextColor(onTrack ? getColor(R.color.btn_plus) : getColor(R.color.btn_minus));
            tvS.setTextSize(12f);

            bot.addView(tvG); bot.addView(tvS);
            row.addView(bot);
        }
        projectionContainer.addView(row);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private View makeRow(String left, String right, boolean header) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 10, 0, 10);
        TextView tvL = new TextView(this);
        tvL.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvL.setText(left);
        tvL.setTextColor(getColor(header ? R.color.text_secondary : R.color.text_primary));
        tvL.setTextSize(header ? 11f : 13f);
        if (header) tvL.setAllCaps(true);
        TextView tvR = new TextView(this);
        tvR.setText(right);
        tvR.setTextColor(getColor(header ? R.color.text_secondary : R.color.accent_blue));
        tvR.setTextSize(header ? 11f : 13f);
        tvR.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        if (header) tvR.setAllCaps(true);
        row.addView(tvL); row.addView(tvR);
        return row;
    }

    private View makeDivider() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(getColor(R.color.divider));
        return v;
    }

    private String shortLabel(String period) {
        if (period == null) return "";
        String[] mo = {"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        if (period.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = period.split("-");
            return mo[Integer.parseInt(p[1])] + " " + p[2];
        } else if (period.matches("\\d{4}-W\\d{2}")) {
            return period.substring(5);
        } else if (period.matches("\\d{4}-\\d{2}")) {
            String[] p = period.split("-");
            return mo[Integer.parseInt(p[1])];
        }
        return period;
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void showExportDialog() {
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                if (allTasks.isEmpty()) { Toast.makeText(this, "No tasks", Toast.LENGTH_SHORT).show(); return; }

                Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                LinearLayout root = new LinearLayout(this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackground(getDrawable(R.drawable.rounded_surface));
                root.setPadding(24, 24, 24, 24);

                TextView title = new TextView(this);
                title.setText("Select Tasks to Export");
                title.setTextColor(getColor(R.color.text_primary));
                title.setTextSize(17f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setPadding(0, 0, 0, 20);
                root.addView(title);

                List<CheckBox> cbs = new ArrayList<>();
                for (Task t : allTasks) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(t.name);
                    cb.setTextColor(getColor(R.color.text_primary));
                    cb.setTextSize(14f);
                    cb.setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.accent_blue)));
                    cb.setChecked(t.id == taskId);
                    cb.setPadding(0, 8, 0, 8);
                    root.addView(cb);
                    cbs.add(cb);
                }

                LinearLayout btns = new LinearLayout(this);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                blp.setMargins(0, 20, 0, 0);
                btns.setLayoutParams(blp);

                TextView btnC = new TextView(this);
                btnC.setLayoutParams(new LinearLayout.LayoutParams(0, 44, 1f));
                btnC.setGravity(Gravity.CENTER);
                btnC.setText("CANCEL");
                btnC.setTextColor(getColor(R.color.text_secondary));
                btnC.setTextSize(13f);
                btnC.setBackground(getDrawable(R.drawable.btn_dialog_cancel));
                ((LinearLayout.LayoutParams)btnC.getLayoutParams()).setMarginEnd(8);

                TextView btnE = new TextView(this);
                btnE.setLayoutParams(new LinearLayout.LayoutParams(0, 44, 1f));
                btnE.setGravity(Gravity.CENTER);
                btnE.setText("EXPORT");
                btnE.setTextColor(getColor(R.color.text_primary));
                btnE.setTextSize(13f);
                btnE.setBackground(getDrawable(R.drawable.btn_dialog_save));

                btns.addView(btnC); btns.addView(btnE);
                root.addView(btns);
                dialog.setContentView(root);
                dialog.getWindow().setLayout(
                        (int)(getResources().getDisplayMetrics().widthPixels * 0.85f),
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                btnC.setOnClickListener(v -> dialog.dismiss());
                btnE.setOnClickListener(v -> {
                    List<Task> sel = new ArrayList<>();
                    for (int i = 0; i < cbs.size(); i++) if (cbs.get(i).isChecked()) sel.add(allTasks.get(i));
                    if (sel.isEmpty()) { Toast.makeText(this, "Select at least one", Toast.LENGTH_SHORT).show(); return; }
                    dialog.dismiss();
                    exportCsv(sel);
                });
                dialog.show();
            });
        });
    }

    private void exportCsv(List<Task> selected) {
        executor.execute(() -> {
            try {
                File dir = new File(getCacheDir(), "exports");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, "HeroCounter_export.csv");
                FileWriter w = new FileWriter(f);
                w.append("Task,Date,Week,Month,Year,Delta,Timestamp\n");
                for (Task t : selected) {
                    for (CountEntry e : db.countEntryDao().getAllEntriesForTask(t.id)) {
                        w.append(esc(t.name)).append(",")
                         .append(e.dateDay).append(",").append(e.dateWeek).append(",")
                         .append(e.dateMonth).append(",").append(e.dateYear).append(",")
                         .append(String.valueOf(e.delta)).append(",")
                         .append(String.valueOf(e.timestamp)).append("\n");
                    }
                }
                w.flush(); w.close();
                Uri uri = FileProvider.getUriForFile(this, "com.herocounter.app.fileprovider", f);
                Intent si = new Intent(Intent.ACTION_SEND);
                si.setType("text/csv");
                si.putExtra(Intent.EXTRA_STREAM, uri);
                si.putExtra(Intent.EXTRA_SUBJECT, "Hero Counter Export");
                si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(si, "Share CSV via…")));
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String esc(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }
}
