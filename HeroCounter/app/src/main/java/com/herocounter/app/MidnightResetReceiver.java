package com.herocounter.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

/**
 * Fires at midnight every day to reset the on-screen counter display.
 * No data is ever deleted — the new day simply starts with a fresh count of 0.
 *
 * Uses USE_EXACT_ALARM (auto-granted, no user prompt) on API 33+.
 * Falls back to setWindow() on API 31-32, and setInexactRepeating on older devices.
 */
public class MidnightResetReceiver extends BroadcastReceiver {

    static final String ACTION_MIDNIGHT_RESET = "com.herocounter.app.MIDNIGHT_RESET";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Re-broadcast locally so MainActivity can react if foregrounded
        Intent local = new Intent(ACTION_MIDNIGHT_RESET);
        context.sendBroadcast(local);

        // Re-schedule for the next midnight
        scheduleMidnightAlarm(context);
    }

    static void scheduleMidnightAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, MidnightResetReceiver.class);
        intent.setAction(ACTION_MIDNIGHT_RESET);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Compute next midnight
        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_YEAR, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long triggerAt = midnight.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ — USE_EXACT_ALARM is auto-granted, safe to call setExactAndAllowWhileIdle
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31-32 — use setWindow for a ~1 min window around midnight (no permission needed)
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000L, pi);
        } else {
            // API 26-30 — setExactAndAllowWhileIdle works without special permission
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
