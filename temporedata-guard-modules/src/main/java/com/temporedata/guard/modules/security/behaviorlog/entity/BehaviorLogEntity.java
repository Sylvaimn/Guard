/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.behaviorlog.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_behavior_log")
public class BehaviorLogEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=32)
    private String userId;
    @Column(length=64)
    private String username;
    @Column(length=128)
    private String action;
    @Column(length=64)
    private String targetType;
    @Column(length=32)
    private String targetId;
    @Column(length=256)
    private String targetName;
    @Column(length=2048)
    private String detail;
    @Column(length=64)
    private String ip;
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

    public String getUsername() {
        return this.username;
    }

    public String getAction() {
        return this.action;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public String getDetail() {
        return this.detail;
    }

    public String getIp() {
        return this.ip;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
        if (!(o instanceof BehaviorLogEntity)) {
            return false;
        }
        BehaviorLogEntity other = (BehaviorLogEntity)o;
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
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$targetId = this.getTargetId();
        String other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !this$targetId.equals(other$targetId)) {
            return false;
        }
        String this$targetName = this.getTargetName();
        String other$targetName = other.getTargetName();
        if (this$targetName == null ? other$targetName != null : !this$targetName.equals(other$targetName)) {
            return false;
        }
        String this$detail = this.getDetail();
        String other$detail = other.getDetail();
        if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) {
            return false;
        }
        String this$ip = this.getIp();
        String other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip)) {
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
        return other instanceof BehaviorLogEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : $targetId.hashCode());
        String $targetName = this.getTargetName();
        result = result * 59 + ($targetName == null ? 43 : $targetName.hashCode());
        String $detail = this.getDetail();
        result = result * 59 + ($detail == null ? 43 : $detail.hashCode());
        String $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : $ip.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "BehaviorLogEntity(id=" + this.getId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", action=" + this.getAction() + ", targetType=" + this.getTargetType() + ", targetId=" + this.getTargetId() + ", targetName=" + this.getTargetName() + ", detail=" + this.getDetail() + ", ip=" + this.getIp() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

