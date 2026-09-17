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
@Table(name="temporedata_guard_alarm_record")
public class AlarmRecordEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String configId;
    @Column(nullable=false, length=32)
    private String eventType;
    @Column(length=1024)
    private String message;
    @Column(nullable=false, length=16)
    private String status;
    @Column(nullable=false)
    private LocalDateTime sendTime;
    @Column(nullable=false, length=32)
    private String tenantId;

    public String getId() {
        return this.id;
    }

    public String getConfigId() {
        return this.configId;
    }

    public String getEventType() {
        return this.eventType;
    }

    public String getMessage() {
        return this.message;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getSendTime() {
        return this.sendTime;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AlarmRecordEntity)) {
            return false;
        }
        AlarmRecordEntity other = (AlarmRecordEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$configId = this.getConfigId();
        String other$configId = other.getConfigId();
        if (this$configId == null ? other$configId != null : !this$configId.equals(other$configId)) {
            return false;
        }
        String this$eventType = this.getEventType();
        String other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        LocalDateTime this$sendTime = this.getSendTime();
        LocalDateTime other$sendTime = other.getSendTime();
        if (this$sendTime == null ? other$sendTime != null : !((Object)this$sendTime).equals(other$sendTime)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        return !(this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AlarmRecordEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $configId = this.getConfigId();
        result = result * 59 + ($configId == null ? 43 : $configId.hashCode());
        String $eventType = this.getEventType();
        result = result * 59 + ($eventType == null ? 43 : $eventType.hashCode());
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        LocalDateTime $sendTime = this.getSendTime();
        result = result * 59 + ($sendTime == null ? 43 : ((Object)$sendTime).hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        return result;
    }

    public String toString() {
        return "AlarmRecordEntity(id=" + this.getId() + ", configId=" + this.getConfigId() + ", eventType=" + this.getEventType() + ", message=" + this.getMessage() + ", status=" + this.getStatus() + ", sendTime=" + this.getSendTime() + ", tenantId=" + this.getTenantId() + ")";
    }
}

