/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.approval.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_approval")
public class ApprovalEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=32)
    private String workflowId;
    @Column(nullable=false, length=128)
    private String title;
    @Column(length=512)
    private String description;
    @Column(nullable=false, length=32)
    private String status;
    @Column(length=32)
    private String applicantId;
    @Column(length=32)
    private String approverId;
    @Column(length=512)
    private String comment;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return this.id;
    }

    public String getWorkflowId() {
        return this.workflowId;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public String getApplicantId() {
        return this.applicantId;
    }

    public String getApproverId() {
        return this.approverId;
    }

    public String getComment() {
        return this.comment;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ApprovalEntity)) {
            return false;
        }
        ApprovalEntity other = (ApprovalEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$workflowId = this.getWorkflowId();
        String other$workflowId = other.getWorkflowId();
        if (this$workflowId == null ? other$workflowId != null : !this$workflowId.equals(other$workflowId)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$applicantId = this.getApplicantId();
        String other$applicantId = other.getApplicantId();
        if (this$applicantId == null ? other$applicantId != null : !this$applicantId.equals(other$applicantId)) {
            return false;
        }
        String this$approverId = this.getApproverId();
        String other$approverId = other.getApproverId();
        if (this$approverId == null ? other$approverId != null : !this$approverId.equals(other$approverId)) {
            return false;
        }
        String this$comment = this.getComment();
        String other$comment = other.getComment();
        if (this$comment == null ? other$comment != null : !this$comment.equals(other$comment)) {
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
        LocalDateTime this$updateTime = this.getUpdateTime();
        LocalDateTime other$updateTime = other.getUpdateTime();
        return !(this$updateTime == null ? other$updateTime != null : !((Object)this$updateTime).equals(other$updateTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ApprovalEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $workflowId = this.getWorkflowId();
        result = result * 59 + ($workflowId == null ? 43 : $workflowId.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $applicantId = this.getApplicantId();
        result = result * 59 + ($applicantId == null ? 43 : $applicantId.hashCode());
        String $approverId = this.getApproverId();
        result = result * 59 + ($approverId == null ? 43 : $approverId.hashCode());
        String $comment = this.getComment();
        result = result * 59 + ($comment == null ? 43 : $comment.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "ApprovalEntity(id=" + this.getId() + ", workflowId=" + this.getWorkflowId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", applicantId=" + this.getApplicantId() + ", approverId=" + this.getApproverId() + ", comment=" + this.getComment() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

