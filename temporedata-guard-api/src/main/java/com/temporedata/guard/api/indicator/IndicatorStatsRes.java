package com.temporedata.guard.api.indicator;

import lombok.Data;

/**
 * Indicator execution statistics for the dashboard cards.
 */
@Data
public class IndicatorStatsRes {

    private int totalRuns;

    private int successRuns;

    private int failedRuns;
}