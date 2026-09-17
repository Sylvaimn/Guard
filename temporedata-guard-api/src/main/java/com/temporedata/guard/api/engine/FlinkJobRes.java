package com.temporedata.guard.api.engine;

import lombok.Data;

/**
 * Flink job response.
 */
@Data
public class FlinkJobRes {

    private String id;

    private String name;

    private String type;

    private String status; // READY, RUNNING, FINISHED, FAILED, CANCELED

    private String jobId; // Flink job id

    private String parallelism;

    private String lastRunTime;

    private String duration;

    private String savepointPath;

    private String errorMsg;

    /** 作业输出日志 / DRY_RUN 提交计划 */
    private String log;

    private String createTime;
}