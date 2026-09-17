package com.temporedata.guard.modules.dev.sync.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Database sync task definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_sync_task", indexes = {
        @Index(name = "idx_st_tenant", columnList = "tenantId"),
        @Index(name = "idx_st_status", columnList = "status")
    })
public class SyncTaskEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String sourceDatasourceId;

    @Column(nullable = false, length = 64)
    private String sourceTable;

    @Column(nullable = false, length = 32)
    private String targetDatasourceId;

    @Column(nullable = false, length = 64)
    private String targetTable;

    @Column(nullable = false, length = 16)
    private String syncMode; // FULL, INCREMENTAL

    @Column(length = 64)
    private String incrementalColumn;

    @Column(length = 128)
    private String incrementalValue;

    /** upsert 主键/唯一键（INCREMENTAL/CDC 模式防重复） */
    @Column(length = 64)
    private String upsertKey;

    /** 轮询 cron（INCREMENTAL 模式定时增量拉取，如 每5分钟一次） */
    @Column(length = 64)
    private String scheduleCron;

    @Column(length = 8)
    private String batchSize; // default 1000

    @Column(length = 512)
    private String config;

    @Column(nullable = false, length = 16)
    private String status; // READY, RUNNING, SUCCESS, FAILED

    private Integer rowCount;

    private Long durationMs;

    @Column(length = 1024)
    private String errorMsg;

    private LocalDateTime lastRunTime;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}