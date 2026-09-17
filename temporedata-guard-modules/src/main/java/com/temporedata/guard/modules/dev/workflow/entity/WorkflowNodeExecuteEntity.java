package com.temporedata.guard.modules.dev.workflow.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Per-node execution result within a workflow run.
 */
@Data
@Entity
@Table(name = "temporedata_guard_workflow_node_execute")
public class WorkflowNodeExecuteEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String executeId;

    @Column(nullable = false, length = 32)
    private String nodeId;

    @Column(nullable = false, length = 64)
    private String nodeName;

    @Column(nullable = false, length = 16)
    private String status; // SUCCESS, FAILED, SKIPPED

    private long durationMs;

    @Column(nullable = false)
    private Integer retryTimes = 0;

    @Column(length = 1024)
    private String errorMsg;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;
}