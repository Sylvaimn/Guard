package com.temporedata.guard.modules.dev.engine.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Flink job definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_flink_job")
public class FlinkJobEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String type; // JAR, SQL, PYTHON

    @Column(length = 512)
    private String jarPath;

    @Column(length = 256)
    private String mainClass;

    @Column(length = 4096)
    private String sqlContent;

    @Column(length = 512)
    private String pythonFile;

    @Column(length = 256)
    private String flinkHome;

    @Column(length = 8)
    private String parallelism;

    @Column(length = 16)
    private String jobManagerMemory;

    @Column(length = 16)
    private String taskManagerMemory;

    private Integer taskSlots;

    @Column(length = 512)
    private String savepointPath;

    @Column(length = 512)
    private String args;

    @Column(length = 1024)
    private String conf;

    @Column(nullable = false, length = 16)
    private String status; // READY, RUNNING, FINISHED, FAILED, CANCELED

    @Column(length = 64)
    private String jobId; // Flink job id

    private LocalDateTime lastRunTime;

    private Long durationMs;

    @Column(length = 2048)
    private String errorMsg;

    /** 作业输出日志(DRY_RUN 时为提交计划) */
    @javax.persistence.Column(length = 8000)
    private String log;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}