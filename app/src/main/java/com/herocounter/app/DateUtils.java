package com.herocounter.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // SimpleDateFormat is not thread-safe. These formatters are called from several
    // Activities' background executors, which can run concurrently, so each thread
    // gets its own instance via ThreadLocal to avoid corrupted output / crashes.
    private static final ThreadLocal<SimpleDateFormat> FMT_DAY =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd", Locale.US));
    private static final ThreadLocal<SimpleDateFormat> FMT_MONTH =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM", Locale.US));
    private static final ThreadLocal<SimpleDateFormat> FMT_YEAR =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy", Locale.US));

    public static String today() {
        return FMT_DAY.get().format(new Date());
    }

    public static String yesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return FMT_DAY.get().format(cal.getTime());
    }

    public static String currentWeek() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setMinimalDaysInFirstWeek(4);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        // Handle year boundary
        if (cal.get(Calendar.MONTH) == Calendar.DECEMBER && week == 1) year++;
        if (cal.get(Calendar.MONTH) == Calendar.JANUARY && week >= 52) year--;
        return String.format(Locale.US, "%04d-W%02d", year, week);
    }

    public static String currentMonth() {
        return FMT_MONTH.get().format(new Date());
    }

    public static String currentYear() {
        return FMT_YEAR.get().format(new Date());
    }

    public static String weekForTimestamp(long ts) {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTimeInMillis(ts);
        cal.setMinimalDaysInFirstWeek(4);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        if (cal.get(Calendar.MONTH) == Calendar.DECEMBER && week == 1) year++;
        if (cal.get(Calendar.MONTH) == Calendar.JANUARY && week >= 52) year--;
        return String.format(Locale.US, "%04d-W%02d", year, week);
    }

    public static String dayForTimestamp(long ts) {
        return FMT_DAY.get().format(new Date(ts));
    }

    public static String monthForTimestamp(long ts) {
        return FMT_MONTH.get().format(new Date(ts));
    }

    public static String yearForTimestamp(long ts) {
        return FMT_YEAR.get().format(new Date(ts));
    }
}
