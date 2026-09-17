/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 */
package com.temporedata.guard.modules.security.alarm.event;

import org.springframework.context.ApplicationEvent;

public class QualityCheckFailedEvent
extends ApplicationEvent {
    private final String ruleId;
    private final String ruleName;
    private final String tenantId;
    private final String errorMessage;

    public QualityCheckFailedEvent(Object source, String ruleId, String ruleName, String tenantId, String errorMessage) {
        super(source);
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.tenantId = tenantId;
        this.errorMessage = errorMessage;
    }

    public String getRuleId() {
        return this.ruleId;
    }

    public String getRuleName() {
        return this.ruleName;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}

