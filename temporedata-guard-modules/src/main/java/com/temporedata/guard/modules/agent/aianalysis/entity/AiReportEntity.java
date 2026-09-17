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
@Table(name="temporedata_guard_ai_report", indexes={@Index(name="idx_ar_tenant_time", columnList="tenantId, createTime"), @Index(name="idx_ar_entity", columnList="entityId")})
public class AiReportEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=32)
    private String taskId;
    @Column(length=32)
    private String entityId;
    @Column(length=64)
    private String entityName;
    @Column(length=16000)
    private String reportContent;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false, length=32)
    private String creatorId;
    @Column(nullable=false)
    private LocalDateTime createTime;

    public String getId() {
        return this.id;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public String getReportContent() {
        return this.reportContent;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getCreatorId() {
        return this.creatorId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AiReportEntity)) {
            return false;
        }
        AiReportEntity other = (AiReportEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$taskId = this.getTaskId();
        String other$taskId = other.getTaskId();
        if (this$taskId == null ? other$taskId != null : !this$taskId.equals(other$taskId)) {
            return false;
        }
        String this$entityId = this.getEntityId();
        String other$entityId = other.getEntityId();
        if (this$entityId == null ? other$entityId != null : !this$entityId.equals(other$entityId)) {
            return false;
        }
        String this$entityName = this.getEntityName();
        String other$entityName = other.getEntityName();
        if (this$entityName == null ? other$entityName != null : !this$entityName.equals(other$entityName)) {
            return false;
        }
        String this$reportContent = this.getReportContent();
        String other$reportContent = other.getReportContent();
        if (this$reportContent == null ? other$reportContent != null : !this$reportContent.equals(other$reportContent)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        String this$creatorId = this.getCreatorId();
        String other$creatorId = other.getCreatorId();
        if (this$creatorId == null ? other$creatorId != null : !this$creatorId.equals(other$creatorId)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        return !(this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AiReportEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $taskId = this.getTaskId();
        result = result * 59 + ($taskId == null ? 43 : $taskId.hashCode());
        String $entityId = this.getEntityId();
        result = result * 59 + ($entityId == null ? 43 : $entityId.hashCode());
        String $entityName = this.getEntityName();
        result = result * 59 + ($entityName == null ? 43 : $entityName.hashCode());
        String $reportContent = this.getReportContent();
        result = result * 59 + ($reportContent == null ? 43 : $reportContent.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        String $creatorId = this.getCreatorId();
        result = result * 59 + ($creatorId == null ? 43 : $creatorId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "AiReportEntity(id=" + this.getId() + ", taskId=" + this.getTaskId() + ", entityId=" + this.getEntityId() + ", entityName=" + this.getEntityName() + ", reportContent=" + this.getReportContent() + ", tenantId=" + this.getTenantId() + ", creatorId=" + this.getCreatorId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

