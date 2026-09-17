package com.temporedata.guard.api.sync;

import lombok.Data;

/**
 * DB sync task response.
 */
@Data
public class SyncTaskRes {

    private String id;

    private String name;

    private String sourceDatasourceId;

    private String sourceTable;

    private String targetDatasourceId;

    private String targetTable;

    private String syncMode; // FULL, INCREMENTAL

    private String incrementalColumn;

    private String incrementalValue;

    private String upsertKey;

    private String scheduleCron;

    private String status; // READY, RUNNING, SUCCESS, FAILED

    private Integer rowCount;

    private String lastRunTime;

    private String duration;

    private String errorMsg;

    private String createTime;
}