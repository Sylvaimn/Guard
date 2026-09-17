/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.alarm.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_alarm_config")
public class AlarmConfigEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=128)
    private String name;
    @Column(nullable=false, length=32)
    private String eventType;
    @Column(length=128)
    private String channels;
    @Column(length=256)
    private String email;
    @Column(length=512)
    private String webhookUrl;
    @Column(nullable=false)
    private Boolean enabled;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getEventType() {
        return this.eventType;
    }

    public String getChannels() {
        return this.channels;
    }

    public String getEmail() {
        return this.email;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public Boolean getEnabled() {
        return this.enabled;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
        if (!(o instanceof AlarmConfigEntity)) {
            return false;
        }
        AlarmConfigEntity other = (AlarmConfigEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$enabled = this.getEnabled();
        Boolean other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !((Object)this$enabled).equals(other$enabled)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$eventType = this.getEventType();
        String other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) {
            return false;
        }
        String this$channels = this.getChannels();
        String other$channels = other.getChannels();
        if (this$channels == null ? other$channels != null : !this$channels.equals(other$channels)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$webhookUrl = this.getWebhookUrl();
        String other$webhookUrl = other.getWebhookUrl();
        if (this$webhookUrl == null ? other$webhookUrl != null : !this$webhookUrl.equals(other$webhookUrl)) {
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
        return other instanceof AlarmConfigEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $eventType = this.getEventType();
        result = result * 59 + ($eventType == null ? 43 : $eventType.hashCode());
        String $channels = this.getChannels();
        result = result * 59 + ($channels == null ? 43 : $channels.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $webhookUrl = this.getWebhookUrl();
        result = result * 59 + ($webhookUrl == null ? 43 : $webhookUrl.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "AlarmConfigEntity(id=" + this.getId() + ", name=" + this.getName() + ", eventType=" + this.getEventType() + ", channels=" + this.getChannels() + ", email=" + this.getEmail() + ", webhookUrl=" + this.getWebhookUrl() + ", enabled=" + this.getEnabled() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

