package com.temporedata.guard.modules.dev.workflow.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Edge (dependency) between two workflow nodes.
 * Supports conditional branching: SUCCESS / FAILURE / ALWAYS.
 */
@Data
@Entity
@Table(name = "temporedata_guard_workflow_edge")
public class WorkflowEdgeEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String workflowId;

    @Column(nullable = false, length = 32)
    private String sourceNodeId;

    @Column(nullable = false, length = 32)
    private String targetNodeId;

    @Column(nullable = false, length = 16)
    private String edgeType = "SUCCESS"; // SUCCESS, FAILURE, ALWAYS

    @Column(length = 256)
    private String conditionExpr; // optional condition expression for conditional branching
}