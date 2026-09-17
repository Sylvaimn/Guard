package com.temporedata.guard.modules.dev.workflow.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Workflow definition (DAG).
 */
@Data
@Entity
@Table(name = "temporedata_guard_workflow")
public class WorkflowEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 16)
    private String status; // OFFLINE, ONLINE

    @Column(length = 64)
    private String cronExpression;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}