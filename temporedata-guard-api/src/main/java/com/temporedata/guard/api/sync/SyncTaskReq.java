package com.temporedata.guard.api.sync;

import lombok.Data;

/**
 * DB sync task request.
 */
@Data
public class SyncTaskReq {

    private String name;

    private String sourceDatasourceId;

    private String sourceTable;

    private String targetDatasourceId;

    private String targetTable;

    private String syncMode; // FULL, INCREMENTAL

    private String incrementalColumn; // column for incremental sync, e.g. update_time, id

    private String incrementalValue; // last sync value for incremental

    private String upsertKey; // upsert 主键/唯一键（INCREMENTAL/CDC 防重复）

    private String scheduleCron; // 轮询 cron（INCREMENTAL 定时增量）

    private String batchSize; // rows per batch, default 1000

    private String config; // extra config as JSON
}