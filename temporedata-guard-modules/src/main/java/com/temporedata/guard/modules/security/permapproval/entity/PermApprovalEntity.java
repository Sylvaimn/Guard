/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.permapproval.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_perm_approval")
public class PermApprovalEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=32)
    private String applicantId;
    @Column(length=64)
    private String applicantName;
    @Column(length=32)
    private String resourceType;
    @Column(length=32)
    private String resourceId;
    @Column(length=128)
    private String resourceName;
    @Column(length=32)
    private String accessType;
    @Column(length=512)
    private String reason;
    @Column(length=16)
    private String status;
    @Column(length=32)
    private String approverId;
    @Column(length=64)
    private String approverName;
    @Column(length=512)
    private String approveComment;
    private LocalDateTime expireTime;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return this.id;
    }

    public String getApplicantId() {
        return this.applicantId;
    }

    public String getApplicantName() {
        return this.applicantName;
    }

    public String getResourceType() {
        return this.resourceType;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public String getAccessType() {
        return this.accessType;
    }

    public String getReason() {
        return this.reason;
    }

    public String getStatus() {
        return this.status;
    }

    public String getApproverId() {
        return this.approverId;
    }

    public String getApproverName() {
        return this.approverName;
    }

    public String getApproveComment() {
        return this.approveComment;
    }

    public LocalDateTime getExpireTime() {
        return this.expireTime;
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

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public void setApproveComment(String approveComment) {
        this.approveComment = approveComment;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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
        if (!(o instanceof PermApprovalEntity)) {
            return false;
        }
        PermApprovalEntity other = (PermApprovalEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$applicantId = this.getApplicantId();
        String other$applicantId = other.getApplicantId();
        if (this$applicantId == null ? other$applicantId != null : !this$applicantId.equals(other$applicantId)) {
            return false;
        }
        String this$applicantName = this.getApplicantName();
        String other$applicantName = other.getApplicantName();
        if (this$applicantName == null ? other$applicantName != null : !this$applicantName.equals(other$applicantName)) {
            return false;
        }
        String this$resourceType = this.getResourceType();
        String other$resourceType = other.getResourceType();
        if (this$resourceType == null ? other$resourceType != null : !this$resourceType.equals(other$resourceType)) {
            return false;
        }
        String this$resourceId = this.getResourceId();
        String other$resourceId = other.getResourceId();
        if (this$resourceId == null ? other$resourceId != null : !this$resourceId.equals(other$resourceId)) {
            return false;
        }
        String this$resourceName = this.getResourceName();
        String other$resourceName = other.getResourceName();
        if (this$resourceName == null ? other$resourceName != null : !this$resourceName.equals(other$resourceName)) {
            return false;
        }
        String this$accessType = this.getAccessType();
        String other$accessType = other.getAccessType();
        if (this$accessType == null ? other$accessType != null : !this$accessType.equals(other$accessType)) {
            return false;
        }
        String this$reason = this.getReason();
        String other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$approverId = this.getApproverId();
        String other$approverId = other.getApproverId();
        if (this$approverId == null ? other$approverId != null : !this$approverId.equals(other$approverId)) {
            return false;
        }
        String this$approverName = this.getApproverName();
        String other$approverName = other.getApproverName();
        if (this$approverName == null ? other$approverName != null : !this$approverName.equals(other$approverName)) {
            return false;
        }
        String this$approveComment = this.getApproveComment();
        String other$approveComment = other.getApproveComment();
        if (this$approveComment == null ? other$approveComment != null : !this$approveComment.equals(other$approveComment)) {
            return false;
        }
        LocalDateTime this$expireTime = this.getExpireTime();
        LocalDateTime other$expireTime = other.getExpireTime();
        if (this$expireTime == null ? other$expireTime != null : !((Object)this$expireTime).equals(other$expireTime)) {
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
        return other instanceof PermApprovalEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $applicantId = this.getApplicantId();
        result = result * 59 + ($applicantId == null ? 43 : $applicantId.hashCode());
        String $applicantName = this.getApplicantName();
        result = result * 59 + ($applicantName == null ? 43 : $applicantName.hashCode());
        String $resourceType = this.getResourceType();
        result = result * 59 + ($resourceType == null ? 43 : $resourceType.hashCode());
        String $resourceId = this.getResourceId();
        result = result * 59 + ($resourceId == null ? 43 : $resourceId.hashCode());
        String $resourceName = this.getResourceName();
        result = result * 59 + ($resourceName == null ? 43 : $resourceName.hashCode());
        String $accessType = this.getAccessType();
        result = result * 59 + ($accessType == null ? 43 : $accessType.hashCode());
        String $reason = this.getReason();
        result = result * 59 + ($reason == null ? 43 : $reason.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $approverId = this.getApproverId();
        result = result * 59 + ($approverId == null ? 43 : $approverId.hashCode());
        String $approverName = this.getApproverName();
        result = result * 59 + ($approverName == null ? 43 : $approverName.hashCode());
        String $approveComment = this.getApproveComment();
        result = result * 59 + ($approveComment == null ? 43 : $approveComment.hashCode());
        LocalDateTime $expireTime = this.getExpireTime();
        result = result * 59 + ($expireTime == null ? 43 : ((Object)$expireTime).hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "PermApprovalEntity(id=" + this.getId() + ", applicantId=" + this.getApplicantId() + ", applicantName=" + this.getApplicantName() + ", resourceType=" + this.getResourceType() + ", resourceId=" + this.getResourceId() + ", resourceName=" + this.getResourceName() + ", accessType=" + this.getAccessType() + ", reason=" + this.getReason() + ", status=" + this.getStatus() + ", approverId=" + this.getApproverId() + ", approverName=" + this.getApproverName() + ", approveComment=" + this.getApproveComment() + ", expireTime=" + this.getExpireTime() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

