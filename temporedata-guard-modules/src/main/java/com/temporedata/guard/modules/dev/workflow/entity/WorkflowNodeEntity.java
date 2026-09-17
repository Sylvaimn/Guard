package com.temporedata.guard.modules.dev.workflow.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * A single task node in a workflow DAG.
 * Supports retry, timeout, priority, and fail strategy.
 */
@Data
@Entity
@Table(name = "temporedata_guard_workflow_node")
public class WorkflowNodeEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String workflowId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type; // SQL, SHELL, PYTHON

    @Column(length = 32)
    private String datasourceId;

    @Column(name = "`sql`", columnDefinition = "TEXT")
    private String sql;

    private Integer positionX;

    private Integer positionY;

    private Integer sortOrder;

    // ---- DolphinScheduler-aligned enhancements ----

    @Column(nullable = false)
    private Integer retryCount = 0; // number of retries, 0 = no retry

    @Column(nullable = false)
    private Integer retryInterval = 1; // retry interval in seconds

    private Integer timeoutSeconds; // task timeout in seconds, null = no timeout

    @Column(nullable = false, length = 16)
    private String priority = "MEDIUM"; // HIGHEST, HIGH, MEDIUM, LOW, LOWEST

    @Column(nullable = false, length = 16)
    private String failStrategy = "STOP"; // STOP, CONTINUE, RETRY

    @Column(length = 512)
    private String params; // JSON custom parameters for the task

    // ---- New node type fields ----

    @Column(length = 1024)
    private String sparkConf; // Spark configuration (SPARK_SQL)

    @Column(length = 512)
    private String httpUrl; // HTTP request URL (HTTP)

    @Column(length = 8)
    private String httpMethod = "GET"; // GET, POST, PUT, DELETE (HTTP)

    @Column(length = 1024)
    private String httpHeaders; // JSON headers (HTTP)

    @Column(length = 32)
    private String dependencyType = "TIME_POINT"; // TIME_POINT, FILE_ARRIVAL, EXTERNAL_TASK (DEPENDENCY)

    private Integer dependencyTimeout = 0; // wait timeout in seconds (DEPENDENCY)

    @Column(length = 256)
    private String emailTo; // recipient email (EMAIL)

    @Column(length = 256)
    private String emailSubject; // email subject (EMAIL)

    @Column(length = 32)
    private String qualityType = "ROW_COUNT"; // ROW_COUNT, NULL_CHECK, DUPLICATE, CUSTOM (DATA_QUALITY)

    private Integer qualityThreshold = 0; // threshold percentage (DATA_QUALITY)

    @Column(length = 512)
    private String scopeSql; // 数据范围条件(防越权): 如 region='昌平区' 或 tenant_id='T01'
}