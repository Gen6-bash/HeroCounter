package com.herocounter.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Schedules and cancels per-task reminder alarms.
 * Each task gets one alarm (its next upcoming occurrence).
 * When it fires, ReminderReceiver re-schedules the next one.
 */
public class ReminderManager {

    public static void scheduleReminder(Context context, Task task) {
        if (!task.reminderEnabled || task.reminderDays == null || task.reminderDays.isEmpty()) {
            cancelReminder(context, task.id);
            return;
        }

        Set<Integer> days = parseDays(task.reminderDays != null ? task.reminderDays : "");
        if (days.isEmpty()) {
            cancelReminder(context, task.id);
            return;
        }

        // Find next trigger time
        Calendar next = nextOccurrence(task.reminderHour, task.reminderMinute, days);
        if (next == null) {
            cancelReminder(context, task.id);
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(context, task);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.setWindow(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), 60_000L, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }

    public static void cancelReminder(Context context, int taskId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, taskId, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    /**
     * Find the next Calendar time matching hour:minute on one of the allowed days.
     * Searches up to 8 days ahead.
     */
    private static Calendar nextOccurrence(int hour, int minute, Set<Integer> allowedDays) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // If today's time has already passed, start searching from tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Search up to 8 days to find next matching day
        for (int i = 0; i < 8; i++) {
            int dow = cal.get(Calendar.DAY_OF_WEEK); // Calendar.SUNDAY=1 … SATURDAY=7
            if (allowedDays.contains(dow)) return cal;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return null;
    }

    private static PendingIntent buildPendingIntent(Context context, Task task) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_NAME, task.name);
        return PendingIntent.getBroadcast(context, task.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static Set<Integer> parseDays(String daysStr) {
        Set<Integer> set = new HashSet<>();
        if (daysStr == null || daysStr.isEmpty()) return set;
        for (String s : daysStr.split(",")) {
            try { set.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        }
        return set;
    }

    public static String daysToString(boolean[] selected) {
        // selected[0]=Mon … selected[6]=Sun → Calendar constants Mon=2…Sun=1
        int[] calDays = {2, 3, 4, 5, 6, 7, 1};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (selected[i]) {
                if (sb.length() > 0) sb.append(",");
                sb.append(calDays[i]);
            }
        }
        return sb.toString();
    }

    public static boolean[] stringToDaysBools(String daysStr) {
        // Returns boolean[7]: [Mon, Tue, Wed, Thu, Fri, Sat, Sun]
        int[] calDays = {2, 3, 4, 5, 6, 7, 1};
        boolean[] result = new boolean[7];
        Set<Integer> set = parseDays(daysStr);
        for (int i = 0; i < 7; i++) {
            result[i] = set.contains(calDays[i]);
        }
        return result;
    }
}
