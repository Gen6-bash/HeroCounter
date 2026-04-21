package com.herocounter.app;

/**
 * Lightweight POJO used by Room aggregation queries.
 * Holds a time period label and the summed count total for that period.
 */
public class PeriodTotal {
    public String period;
    public int total;
}
