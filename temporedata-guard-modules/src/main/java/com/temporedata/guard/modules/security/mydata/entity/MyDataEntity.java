/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.mydata.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_my_data")
public class MyDataEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String userId;
    @Column(length=32)
    private String resourceType;
    @Column(length=32)
    private String resourceId;
    @Column(length=128)
    private String resourceName;
    @Column(length=32)
    private String accessType;
    @Column(length=32)
    private String grantedBy;
    private LocalDateTime expireTime;
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

    public String getGrantedBy() {
        return this.grantedBy;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MyDataEntity)) {
            return false;
        }
        MyDataEntity other = (MyDataEntity)o;
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
        String this$grantedBy = this.getGrantedBy();
        String other$grantedBy = other.getGrantedBy();
        if (this$grantedBy == null ? other$grantedBy != null : !this$grantedBy.equals(other$grantedBy)) {
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
        return !(this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof MyDataEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        String $resourceType = this.getResourceType();
        result = result * 59 + ($resourceType == null ? 43 : $resourceType.hashCode());
        String $resourceId = this.getResourceId();
        result = result * 59 + ($resourceId == null ? 43 : $resourceId.hashCode());
        String $resourceName = this.getResourceName();
        result = result * 59 + ($resourceName == null ? 43 : $resourceName.hashCode());
        String $accessType = this.getAccessType();
        result = result * 59 + ($accessType == null ? 43 : $accessType.hashCode());
        String $grantedBy = this.getGrantedBy();
        result = result * 59 + ($grantedBy == null ? 43 : $grantedBy.hashCode());
        LocalDateTime $expireTime = this.getExpireTime();
        result = result * 59 + ($expireTime == null ? 43 : ((Object)$expireTime).hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "MyDataEntity(id=" + this.getId() + ", userId=" + this.getUserId() + ", resourceType=" + this.getResourceType() + ", resourceId=" + this.getResourceId() + ", resourceName=" + this.getResourceName() + ", accessType=" + this.getAccessType() + ", grantedBy=" + this.getGrantedBy() + ", expireTime=" + this.getExpireTime() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

