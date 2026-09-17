/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.message.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_message")
public class MessageEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String userId;
    @Column(nullable=false, length=32)
    private String type;
    @Column(nullable=false, length=128)
    private String title;
    @Column(length=2048)
    private String content;
    @Column(nullable=false)
    private Integer isRead = 0;
    @Column(length=64)
    private String bizType;
    @Column(length=32)
    private String bizId;
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

    public String getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public Integer getIsRead() {
        return this.isRead;
    }

    public String getBizType() {
        return this.bizType;
    }

    public String getBizId() {
        return this.bizId;
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

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
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
        if (!(o instanceof MessageEntity)) {
            return false;
        }
        MessageEntity other = (MessageEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$isRead = this.getIsRead();
        Integer other$isRead = other.getIsRead();
        if (this$isRead == null ? other$isRead != null : !((Object)this$isRead).equals(other$isRead)) {
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
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        String this$bizType = this.getBizType();
        String other$bizType = other.getBizType();
        if (this$bizType == null ? other$bizType != null : !this$bizType.equals(other$bizType)) {
            return false;
        }
        String this$bizId = this.getBizId();
        String other$bizId = other.getBizId();
        if (this$bizId == null ? other$bizId != null : !this$bizId.equals(other$bizId)) {
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
        return other instanceof MessageEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $isRead = this.getIsRead();
        result = result * 59 + ($isRead == null ? 43 : ((Object)$isRead).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $bizType = this.getBizType();
        result = result * 59 + ($bizType == null ? 43 : $bizType.hashCode());
        String $bizId = this.getBizId();
        result = result * 59 + ($bizId == null ? 43 : $bizId.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "MessageEntity(id=" + this.getId() + ", userId=" + this.getUserId() + ", type=" + this.getType() + ", title=" + this.getTitle() + ", content=" + this.getContent() + ", isRead=" + this.getIsRead() + ", bizType=" + this.getBizType() + ", bizId=" + this.getBizId() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

