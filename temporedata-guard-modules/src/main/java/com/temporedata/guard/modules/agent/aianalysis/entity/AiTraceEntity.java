/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Index
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.agent.aianalysis.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_ai_trace", indexes={@Index(name="idx_at_tenant_time", columnList="tenantId, createTime"), @Index(name="idx_at_user", columnList="userId"), @Index(name="idx_at_session", columnList="sessionId")})
public class AiTraceEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String userId;
    @Column(length=32)
    private String sessionId;
    @Column(nullable=false, length=16)
    private String step;
    @Column(nullable=false, length=64)
    private String action;
    @Column(length=2000)
    private String params;
    @Column(length=2000)
    private String result;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;

    public String getId() {
        return this.id;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public String getStep() {
        return this.step;
    }

    public String getAction() {
        return this.action;
    }

    public String getParams() {
        return this.params;
    }

    public String getResult() {
        return this.result;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AiTraceEntity)) {
            return false;
        }
        AiTraceEntity other = (AiTraceEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$userId = this.getUserId();
        String other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) {
            return false;
        }
        String this$sessionId = this.getSessionId();
        String other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) {
            return false;
        }
        String this$step = this.getStep();
        String other$step = other.getStep();
        if (this$step == null ? other$step != null : !this$step.equals(other$step)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
            return false;
        }
        String this$params = this.getParams();
        String other$params = other.getParams();
        if (this$params == null ? other$params != null : !this$params.equals(other$params)) {
            return false;
        }
        String this$result = this.getResult();
        String other$result = other.getResult();
        if (this$result == null ? other$result != null : !this$result.equals(other$result)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        return !(this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AiTraceEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        String $sessionId = this.getSessionId();
        result = result * 59 + ($sessionId == null ? 43 : $sessionId.hashCode());
        String $step = this.getStep();
        result = result * 59 + ($step == null ? 43 : $step.hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        String $params = this.getParams();
        result = result * 59 + ($params == null ? 43 : $params.hashCode());
        String $result = this.getResult();
        result = result * 59 + ($result == null ? 43 : $result.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "AiTraceEntity(id=" + this.getId() + ", userId=" + this.getUserId() + ", sessionId=" + this.getSessionId() + ", step=" + this.getStep() + ", action=" + this.getAction() + ", params=" + this.getParams() + ", result=" + this.getResult() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

