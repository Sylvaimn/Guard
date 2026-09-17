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
package com.temporedata.guard.modules.security.security.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_audit_log", indexes={@Index(name="idx_al_tenant_time", columnList="tenantId, createTime")})
public class AuditLogEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=64)
    private String operator;
    @Column(nullable=false, length=32)
    private String actionType;
    @Column(length=512)
    private String actionDetail;
    @Column(length=32)
    private String targetType;
    @Column(length=128)
    private String targetName;
    @Column(length=64)
    private String ipAddress;
    @Column(nullable=false, length=16)
    private String result;
    @Column(length=1024)
    private String errorMsg;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private Long durationMs;

    public String getId() {
        return this.id;
    }

    public String getOperator() {
        return this.operator;
    }

    public String getActionType() {
        return this.actionType;
    }

    public String getActionDetail() {
        return this.actionDetail;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getResult() {
        return this.result;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Long getDurationMs() {
        return this.durationMs;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setActionDetail(String actionDetail) {
        this.actionDetail = actionDetail;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AuditLogEntity)) {
            return false;
        }
        AuditLogEntity other = (AuditLogEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$durationMs = this.getDurationMs();
        Long other$durationMs = other.getDurationMs();
        if (this$durationMs == null ? other$durationMs != null : !((Object)this$durationMs).equals(other$durationMs)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$operator = this.getOperator();
        String other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) {
            return false;
        }
        String this$actionType = this.getActionType();
        String other$actionType = other.getActionType();
        if (this$actionType == null ? other$actionType != null : !this$actionType.equals(other$actionType)) {
            return false;
        }
        String this$actionDetail = this.getActionDetail();
        String other$actionDetail = other.getActionDetail();
        if (this$actionDetail == null ? other$actionDetail != null : !this$actionDetail.equals(other$actionDetail)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$targetName = this.getTargetName();
        String other$targetName = other.getTargetName();
        if (this$targetName == null ? other$targetName != null : !this$targetName.equals(other$targetName)) {
            return false;
        }
        String this$ipAddress = this.getIpAddress();
        String other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress)) {
            return false;
        }
        String this$result = this.getResult();
        String other$result = other.getResult();
        if (this$result == null ? other$result != null : !this$result.equals(other$result)) {
            return false;
        }
        String this$errorMsg = this.getErrorMsg();
        String other$errorMsg = other.getErrorMsg();
        if (this$errorMsg == null ? other$errorMsg != null : !this$errorMsg.equals(other$errorMsg)) {
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
        return other instanceof AuditLogEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $durationMs = this.getDurationMs();
        result = result * 59 + ($durationMs == null ? 43 : ((Object)$durationMs).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $operator = this.getOperator();
        result = result * 59 + ($operator == null ? 43 : $operator.hashCode());
        String $actionType = this.getActionType();
        result = result * 59 + ($actionType == null ? 43 : $actionType.hashCode());
        String $actionDetail = this.getActionDetail();
        result = result * 59 + ($actionDetail == null ? 43 : $actionDetail.hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $targetName = this.getTargetName();
        result = result * 59 + ($targetName == null ? 43 : $targetName.hashCode());
        String $ipAddress = this.getIpAddress();
        result = result * 59 + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        String $result = this.getResult();
        result = result * 59 + ($result == null ? 43 : $result.hashCode());
        String $errorMsg = this.getErrorMsg();
        result = result * 59 + ($errorMsg == null ? 43 : $errorMsg.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "AuditLogEntity(id=" + this.getId() + ", operator=" + this.getOperator() + ", actionType=" + this.getActionType() + ", actionDetail=" + this.getActionDetail() + ", targetType=" + this.getTargetType() + ", targetName=" + this.getTargetName() + ", ipAddress=" + this.getIpAddress() + ", result=" + this.getResult() + ", errorMsg=" + this.getErrorMsg() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", durationMs=" + this.getDurationMs() + ")";
    }
}

