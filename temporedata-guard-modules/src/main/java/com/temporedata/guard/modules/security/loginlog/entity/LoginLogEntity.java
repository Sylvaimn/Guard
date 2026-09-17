/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.loginlog.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_login_log")
public class LoginLogEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=32)
    private String userId;
    @Column(length=64)
    private String username;
    @Column(length=32)
    private String loginType;
    @Column(length=64)
    private String loginIp;
    @Column(length=512)
    private String userAgent;
    @Column(length=16)
    private String status;
    @Column(length=256)
    private String failReason;
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

    public String getLoginType() {
        return this.loginType;
    }

    public String getLoginIp() {
        return this.loginIp;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public String getStatus() {
        return this.status;
    }

    public String getFailReason() {
        return this.failReason;
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

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
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
        if (!(o instanceof LoginLogEntity)) {
            return false;
        }
        LoginLogEntity other = (LoginLogEntity)o;
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
        String this$loginType = this.getLoginType();
        String other$loginType = other.getLoginType();
        if (this$loginType == null ? other$loginType != null : !this$loginType.equals(other$loginType)) {
            return false;
        }
        String this$loginIp = this.getLoginIp();
        String other$loginIp = other.getLoginIp();
        if (this$loginIp == null ? other$loginIp != null : !this$loginIp.equals(other$loginIp)) {
            return false;
        }
        String this$userAgent = this.getUserAgent();
        String other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$failReason = this.getFailReason();
        String other$failReason = other.getFailReason();
        if (this$failReason == null ? other$failReason != null : !this$failReason.equals(other$failReason)) {
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
        return other instanceof LoginLogEntity;
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
        String $loginType = this.getLoginType();
        result = result * 59 + ($loginType == null ? 43 : $loginType.hashCode());
        String $loginIp = this.getLoginIp();
        result = result * 59 + ($loginIp == null ? 43 : $loginIp.hashCode());
        String $userAgent = this.getUserAgent();
        result = result * 59 + ($userAgent == null ? 43 : $userAgent.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $failReason = this.getFailReason();
        result = result * 59 + ($failReason == null ? 43 : $failReason.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "LoginLogEntity(id=" + this.getId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", loginType=" + this.getLoginType() + ", loginIp=" + this.getLoginIp() + ", userAgent=" + this.getUserAgent() + ", status=" + this.getStatus() + ", failReason=" + this.getFailReason() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

