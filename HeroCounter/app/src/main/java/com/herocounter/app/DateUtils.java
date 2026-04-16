package com.herocounter.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final SimpleDateFormat FMT_DAY   = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat FMT_MONTH = new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat FMT_YEAR  = new SimpleDateFormat("yyyy", Locale.US);

    public static String today() {
        return FMT_DAY.format(new Date());
    }

    public static String yesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return FMT_DAY.format(cal.getTime());
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
        return FMT_MONTH.format(new Date());
    }

    public static String currentYear() {
        return FMT_YEAR.format(new Date());
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
        return FMT_DAY.format(new Date(ts));
    }

    public static String monthForTimestamp(long ts) {
        return FMT_MONTH.format(new Date(ts));
    }

    public static String yearForTimestamp(long ts) {
        return FMT_YEAR.format(new Date(ts));
    }
}
