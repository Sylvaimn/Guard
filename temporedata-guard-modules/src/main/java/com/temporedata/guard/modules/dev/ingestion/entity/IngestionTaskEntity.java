package com.temporedata.guard.modules.dev.ingestion.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Data ingestion task definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_ingestion_task", indexes = {
        @Index(name = "idx_it_tenant", columnList = "tenantId"),
        @Index(name = "idx_it_status", columnList = "status")
    })
public class IngestionTaskEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String type; // CSV_FILE, JSON_FILE, API

    @Column(nullable = false, length = 32)
    private String datasourceId;

    @Column(nullable = false, length = 64)
    private String targetTable;

    @Column(length = 512)
    private String config;

    @Column(nullable = false, length = 16)
    private String status; // READY, RUNNING, SUCCESS, FAILED

    private Integer rowCount;

    @Column(length = 1024)
    private String errorMsg;

    private LocalDateTime lastRunTime;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}