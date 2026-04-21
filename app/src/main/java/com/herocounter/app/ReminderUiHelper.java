package com.herocounter.app;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

/**
 * Manages the reminder toggle + time picker + day chips inside a dialog.
 *
 * Usage:
 *   1. Inflate your dialog layout into a View (or use dialog.setContentView first)
 *   2. Call bind(dialogRootView) — pass the root view of the inflated layout
 *   3. Optionally call populate(task) to pre-fill existing settings
 *   4. Call applyTo(task) before saving to write values back
 */
public class ReminderUiHelper {

    private final Context context;
    private Switch switchReminder;
    private View reminderOptions;
    private TextView tvTime;
    private TextView[] dayChips;

    private boolean enabled  = false;
    private int hour         = 9;
    private int minute       = 0;
    private boolean[] days   = {true, true, true, true, true, false, false}; // Mon–Fri default

    public ReminderUiHelper(Context context) {
        this.context = context;
    }

    /**
     * Bind to views. Pass the ROOT view of the inflated dialog layout —
     * not the decor view, not the window. e.g. the View returned by
     * LayoutInflater.inflate() or dialog.findViewById(R.id.someRootLayout).
     */
    public void bind(View root) {
        if (root == null) return;

        switchReminder  = root.findViewById(R.id.switchReminder);
        reminderOptions = root.findViewById(R.id.reminderOptions);
        tvTime          = root.findViewById(R.id.tvSelectedTime);
        View timeRow    = root.findViewById(R.id.timePickerRow);

        // If views not found in this root, nothing to do
        if (switchReminder == null || reminderOptions == null) return;

        dayChips = new TextView[]{
            root.findViewById(R.id.dayMon),
            root.findViewById(R.id.dayTue),
            root.findViewById(R.id.dayWed),
            root.findViewById(R.id.dayThu),
            root.findViewById(R.id.dayFri),
            root.findViewById(R.id.daySat),
            root.findViewById(R.id.daySun),
        };

        updateTimeLabel();
        updateDayChips();

        switchReminder.setOnCheckedChangeListener((btn, checked) -> {
            enabled = checked;
            reminderOptions.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        if (timeRow != null) {
            timeRow.setOnClickListener(v ->
                new TimePickerDialog(context, (tp, h, m) -> {
                    hour   = h;
                    minute = m;
                    updateTimeLabel();
                }, hour, minute, false).show()
            );
        }

        for (int i = 0; i < 7; i++) {
            if (dayChips[i] == null) continue;
            final int idx = i;
            dayChips[i].setOnClickListener(v -> {
                days[idx] = !days[idx];
                updateDayChips();
            });
        }
    }

    public void populate(Task task) {
        if (switchReminder == null) return;
        enabled = task.reminderEnabled;
        hour    = task.reminderHour;
        minute  = task.reminderMinute;
        days    = ReminderManager.stringToDaysBools(
                task.reminderDays != null ? task.reminderDays : "2,3,4,5,6,7,1");

        switchReminder.setChecked(enabled);
        reminderOptions.setVisibility(enabled ? View.VISIBLE : View.GONE);
        updateTimeLabel();
        updateDayChips();
    }

    public void applyTo(Task task) {
        task.reminderEnabled = enabled;
        task.reminderHour    = hour;
        task.reminderMinute  = minute;
        task.reminderDays    = ReminderManager.daysToString(days);
    }

    private void updateTimeLabel() {
        if (tvTime == null) return;
        String ampm = hour < 12 ? "AM" : "PM";
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        tvTime.setText(String.format(Locale.US, "%d:%02d %s", h12, minute, ampm));
    }

    private void updateDayChips() {
        if (dayChips == null) return;
        for (int i = 0; i < 7; i++) {
            if (dayChips[i] == null) continue;
            boolean sel = days[i];
            dayChips[i].setBackgroundResource(sel
                    ? R.drawable.day_chip_selected
                    : R.drawable.day_chip_unselected);
            dayChips[i].setTextColor(context.getColor(sel
                    ? R.color.text_primary
                    : R.color.text_muted));
        }
    }
}
