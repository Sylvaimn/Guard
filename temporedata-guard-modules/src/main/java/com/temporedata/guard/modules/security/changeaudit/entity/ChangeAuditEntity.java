/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.changeaudit.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_change_audit")
public class ChangeAuditEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(name="entity_type", nullable=false, length=32)
    private String entityType;
    @Column(name="entity_id", nullable=false, length=64)
    private String entityId;
    @Column(name="entity_name", length=128)
    private String entityName;
    @Column(nullable=false, length=32)
    private String action;
    @Column(length=64)
    private String operator;
    @Column(name="before_value", length=2048)
    private String beforeValue;
    @Column(name="after_value", length=2048)
    private String afterValue;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(name="create_time", nullable=false)
    private LocalDateTime createTime;

    public String getId() {
        return this.id;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public String getAction() {
        return this.action;
    }

    public String getOperator() {
        return this.operator;
    }

    public String getBeforeValue() {
        return this.beforeValue;
    }

    public String getAfterValue() {
        return this.afterValue;
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

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
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
        if (!(o instanceof ChangeAuditEntity)) {
            return false;
        }
        ChangeAuditEntity other = (ChangeAuditEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$entityType = this.getEntityType();
        String other$entityType = other.getEntityType();
        if (this$entityType == null ? other$entityType != null : !this$entityType.equals(other$entityType)) {
            return false;
        }
        String this$entityId = this.getEntityId();
        String other$entityId = other.getEntityId();
        if (this$entityId == null ? other$entityId != null : !this$entityId.equals(other$entityId)) {
            return false;
        }
        String this$entityName = this.getEntityName();
        String other$entityName = other.getEntityName();
        if (this$entityName == null ? other$entityName != null : !this$entityName.equals(other$entityName)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
            return false;
        }
        String this$operator = this.getOperator();
        String other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) {
            return false;
        }
        String this$beforeValue = this.getBeforeValue();
        String other$beforeValue = other.getBeforeValue();
        if (this$beforeValue == null ? other$beforeValue != null : !this$beforeValue.equals(other$beforeValue)) {
            return false;
        }
        String this$afterValue = this.getAfterValue();
        String other$afterValue = other.getAfterValue();
        if (this$afterValue == null ? other$afterValue != null : !this$afterValue.equals(other$afterValue)) {
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
        return other instanceof ChangeAuditEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $entityType = this.getEntityType();
        result = result * 59 + ($entityType == null ? 43 : $entityType.hashCode());
        String $entityId = this.getEntityId();
        result = result * 59 + ($entityId == null ? 43 : $entityId.hashCode());
        String $entityName = this.getEntityName();
        result = result * 59 + ($entityName == null ? 43 : $entityName.hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        String $operator = this.getOperator();
        result = result * 59 + ($operator == null ? 43 : $operator.hashCode());
        String $beforeValue = this.getBeforeValue();
        result = result * 59 + ($beforeValue == null ? 43 : $beforeValue.hashCode());
        String $afterValue = this.getAfterValue();
        result = result * 59 + ($afterValue == null ? 43 : $afterValue.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "ChangeAuditEntity(id=" + this.getId() + ", entityType=" + this.getEntityType() + ", entityId=" + this.getEntityId() + ", entityName=" + this.getEntityName() + ", action=" + this.getAction() + ", operator=" + this.getOperator() + ", beforeValue=" + this.getBeforeValue() + ", afterValue=" + this.getAfterValue() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

