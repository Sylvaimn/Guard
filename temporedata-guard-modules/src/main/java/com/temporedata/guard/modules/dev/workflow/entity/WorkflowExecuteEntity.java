package com.temporedata.guard.modules.dev.workflow.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * A single execution of a workflow.
 */
@Data
@Entity
@Table(name = "temporedata_guard_workflow_execute")
public class WorkflowExecuteEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String workflowId;

    @Column(nullable = false, length = 16)
    private String status; // RUNNING, SUCCESS, FAILED

    @Column
    @Lob
    private String log;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;
}