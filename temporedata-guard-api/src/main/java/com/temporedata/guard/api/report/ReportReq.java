package com.temporedata.guard.api.report;

import lombok.Data;

/**
 * Create / update report request.
 */
@Data
public class ReportReq {

    private String name;

    private String description;

    private String type; // TABLE, CHART, DASHBOARD

    private String config; // JSON config

    private String datasourceId;

    /** 密级(INTERNAL内部/SECRET秘密/TOP_SECRET机密) */
    private String secretLevel;

    /** 指标口径说明 */
    private String caliberDesc;

    /** 生成周期(DAILY日报/WEEKLY周报/MONTHLY月报/MANUAL手动) */
    private String schedulePeriod;
}