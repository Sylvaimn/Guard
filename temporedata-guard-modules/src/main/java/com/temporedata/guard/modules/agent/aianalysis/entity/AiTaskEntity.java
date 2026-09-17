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
@Table(name="temporedata_guard_ai_task", indexes={@Index(name="idx_ait_tenant_status", columnList="tenantId, status"), @Index(name="idx_ait_tenant_time", columnList="tenantId, createTime")})
public class AiTaskEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String type;
    @Column(length=4000)
    private String payload;
    private Integer priority = 5;
    @Column(nullable=false, length=16)
    private String status = "READY";
    @Column(length=1000)
    private String resultRef;
    @Column(length=1000)
    private String errorMsg;
    @Column(length=32)
    private String userId;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getPayload() {
        return this.payload;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public String getStatus() {
        return this.status;
    }

    public String getResultRef() {
        return this.resultRef;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getFinishTime() {
        return this.finishTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setResultRef(String resultRef) {
        this.resultRef = resultRef;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AiTaskEntity)) {
            return false;
        }
        AiTaskEntity other = (AiTaskEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$priority = this.getPriority();
        Integer other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !((Object)this$priority).equals(other$priority)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$payload = this.getPayload();
        String other$payload = other.getPayload();
        if (this$payload == null ? other$payload != null : !this$payload.equals(other$payload)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$resultRef = this.getResultRef();
        String other$resultRef = other.getResultRef();
        if (this$resultRef == null ? other$resultRef != null : !this$resultRef.equals(other$resultRef)) {
            return false;
        }
        String this$errorMsg = this.getErrorMsg();
        String other$errorMsg = other.getErrorMsg();
        if (this$errorMsg == null ? other$errorMsg != null : !this$errorMsg.equals(other$errorMsg)) {
            return false;
        }
        String this$userId = this.getUserId();
        String other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime)) {
            return false;
        }
        LocalDateTime this$startTime = this.getStartTime();
        LocalDateTime other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !((Object)this$startTime).equals(other$startTime)) {
            return false;
        }
        LocalDateTime this$finishTime = this.getFinishTime();
        LocalDateTime other$finishTime = other.getFinishTime();
        return !(this$finishTime == null ? other$finishTime != null : !((Object)this$finishTime).equals(other$finishTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AiTaskEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $priority = this.getPriority();
        result = result * 59 + ($priority == null ? 43 : ((Object)$priority).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $payload = this.getPayload();
        result = result * 59 + ($payload == null ? 43 : $payload.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $resultRef = this.getResultRef();
        result = result * 59 + ($resultRef == null ? 43 : $resultRef.hashCode());
        String $errorMsg = this.getErrorMsg();
        result = result * 59 + ($errorMsg == null ? 43 : $errorMsg.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : ((Object)$startTime).hashCode());
        LocalDateTime $finishTime = this.getFinishTime();
        result = result * 59 + ($finishTime == null ? 43 : ((Object)$finishTime).hashCode());
        return result;
    }

    public String toString() {
        return "AiTaskEntity(id=" + this.getId() + ", type=" + this.getType() + ", payload=" + this.getPayload() + ", priority=" + this.getPriority() + ", status=" + this.getStatus() + ", resultRef=" + this.getResultRef() + ", errorMsg=" + this.getErrorMsg() + ", userId=" + this.getUserId() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", startTime=" + this.getStartTime() + ", finishTime=" + this.getFinishTime() + ")";
    }
}

