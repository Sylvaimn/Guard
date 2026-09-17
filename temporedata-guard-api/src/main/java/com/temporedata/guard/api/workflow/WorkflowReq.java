package com.temporedata.guard.api.workflow;

import lombok.Data;

import java.util.List;

/**
 * Create / update workflow request.
 */
@Data
public class WorkflowReq {

    private String name;

    private String description;

    private List<WorkflowNodeReq> nodes;

    private List<WorkflowEdgeReq> edges;
}