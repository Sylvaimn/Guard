package com.temporedata.guard.modules.common.work.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Independent job instance entity.
 * Represents a single executable job (SparkSQL, FlinkSQL, Python, Bash, etc.)
 * separate from workflow DAG orchestration.
 */
@Data
@Entity
@Table(name = "temporedata_guard_work")
public class WorkEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String workType; // SPARK_SQL, FLINK_SQL, PYTHON, BASH, CURL, API, SPARK_JAR, FLINK_JAR, PRQL, EXCEL_SYNC, SYNC_WORK

    @Column(length = 512)
    private String remark;

    @Column(length = 32)
    private String status; // DRAFT, PUBLISHED, RUNNING, SUCCESS, FAIL, STOPPED

    @Column(length = 32)
    private String configId;

    @Column(length = 32)
    private String clusterId;

    @Column(length = 32)
    private String workflowId;

    @Column(length = 32)
    private String versionId;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON config for the job

    @Column(columnDefinition = "TEXT")
    private String submitLog;

    @Column(columnDefinition = "TEXT")
    private String yarnLog;

    private Integer topIndex;

    @Column(nullable = false, length = 32)
    private String tenantId;

    private LocalDateTime createDateTime;

    private LocalDateTime lastModifiedDateTime;

    @Column(length = 32)
    private String createBy;

    @Column(length = 32)
    private String lastModifiedBy;
}