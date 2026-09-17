/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 */
package com.temporedata.guard.modules.security.alarm.event;

import org.springframework.context.ApplicationEvent;

public class WorkflowFailedEvent
extends ApplicationEvent {
    private final String workflowId;
    private final String workflowName;
    private final String tenantId;
    private final String errorMessage;

    public WorkflowFailedEvent(Object source, String workflowId, String workflowName, String tenantId, String errorMessage) {
        super(source);
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.tenantId = tenantId;
        this.errorMessage = errorMessage;
    }

    public String getWorkflowId() {
        return this.workflowId;
    }

    public String getWorkflowName() {
        return this.workflowName;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}

