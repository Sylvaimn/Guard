package com.temporedata.guard.modules.dev.engine.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Spark job definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_spark_job")
public class SparkJobEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String type; // SPARK_SQL, SPARK_JAR, PYSPARK

    @Column(length = 256)
    private String mainClass;

    @Column(length = 512)
    private String jarPath;

    @Column(length = 4096)
    private String sqlContent;

    @Column(length = 512)
    private String pythonFile;

    @Column(nullable = false, length = 64)
    private String master; // local[*], yarn, spark://host:7077

    @Column(length = 16)
    private String deployMode; // client, cluster

    @Column(length = 16)
    private String driverMemory;

    @Column(length = 16)
    private String executorMemory;

    private Integer executorCores;

    private Integer numExecutors;

    @Column(length = 512)
    private String args;

    @Column(length = 1024)
    private String conf;

    @Column(nullable = false, length = 16)
    private String status; // READY, RUNNING, SUCCESS, FAILED, KILLED

    @Column(length = 64)
    private String appId; // YARN app id or Spark app id

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