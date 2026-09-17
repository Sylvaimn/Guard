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
@Table(name="temporedata_guard_ai_memory", indexes={@Index(name="idx_aim_user_type", columnList="userId, type"), @Index(name="idx_aim_key", columnList="mem_key")})
public class AiMemoryEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String userId;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false, length=32)
    private String type;
    @Column(name="mem_key", nullable=false, length=128)
    private String key;
    @Column(length=2000)
    private String content;
    @Column(nullable=false)
    private LocalDateTime accessTime;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime expireTime;

    public String getId() {
        return this.id;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getType() {
        return this.type;
    }

    public String getKey() {
        return this.key;
    }

    public String getContent() {
        return this.content;
    }

    public LocalDateTime getAccessTime() {
        return this.accessTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getExpireTime() {
        return this.expireTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAccessTime(LocalDateTime accessTime) {
        this.accessTime = accessTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AiMemoryEntity)) {
            return false;
        }
        AiMemoryEntity other = (AiMemoryEntity)o;
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
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$key = this.getKey();
        String other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        LocalDateTime this$accessTime = this.getAccessTime();
        LocalDateTime other$accessTime = other.getAccessTime();
        if (this$accessTime == null ? other$accessTime != null : !((Object)this$accessTime).equals(other$accessTime)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime)) {
            return false;
        }
        LocalDateTime this$expireTime = this.getExpireTime();
        LocalDateTime other$expireTime = other.getExpireTime();
        return !(this$expireTime == null ? other$expireTime != null : !((Object)this$expireTime).equals(other$expireTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AiMemoryEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $key = this.getKey();
        result = result * 59 + ($key == null ? 43 : $key.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        LocalDateTime $accessTime = this.getAccessTime();
        result = result * 59 + ($accessTime == null ? 43 : ((Object)$accessTime).hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $expireTime = this.getExpireTime();
        result = result * 59 + ($expireTime == null ? 43 : ((Object)$expireTime).hashCode());
        return result;
    }

    public String toString() {
        return "AiMemoryEntity(id=" + this.getId() + ", userId=" + this.getUserId() + ", tenantId=" + this.getTenantId() + ", type=" + this.getType() + ", key=" + this.getKey() + ", content=" + this.getContent() + ", accessTime=" + this.getAccessTime() + ", createTime=" + this.getCreateTime() + ", expireTime=" + this.getExpireTime() + ")";
    }
}

