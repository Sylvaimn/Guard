/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.sensitive.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_sensitive_data")
public class SensitiveDataEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=32)
    private String datasourceId;
    @Column(length=128)
    private String tableName;
    @Column(length=128)
    private String columnName;
    @Column(length=32)
    private String sensitiveType;
    @Column(length=32)
    private String maskRule;
    @Column(length=16)
    private String level;
    @Column(length=512)
    private String description;
    @Column(length=16)
    private String status;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return this.id;
    }

    public String getDatasourceId() {
        return this.datasourceId;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public String getSensitiveType() {
        return this.sensitiveType;
    }

    public String getMaskRule() {
        return this.maskRule;
    }

    public String getLevel() {
        return this.level;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
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

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setSensitiveType(String sensitiveType) {
        this.sensitiveType = sensitiveType;
    }

    public void setMaskRule(String maskRule) {
        this.maskRule = maskRule;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
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
        if (!(o instanceof SensitiveDataEntity)) {
            return false;
        }
        SensitiveDataEntity other = (SensitiveDataEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$datasourceId = this.getDatasourceId();
        String other$datasourceId = other.getDatasourceId();
        if (this$datasourceId == null ? other$datasourceId != null : !this$datasourceId.equals(other$datasourceId)) {
            return false;
        }
        String this$tableName = this.getTableName();
        String other$tableName = other.getTableName();
        if (this$tableName == null ? other$tableName != null : !this$tableName.equals(other$tableName)) {
            return false;
        }
        String this$columnName = this.getColumnName();
        String other$columnName = other.getColumnName();
        if (this$columnName == null ? other$columnName != null : !this$columnName.equals(other$columnName)) {
            return false;
        }
        String this$sensitiveType = this.getSensitiveType();
        String other$sensitiveType = other.getSensitiveType();
        if (this$sensitiveType == null ? other$sensitiveType != null : !this$sensitiveType.equals(other$sensitiveType)) {
            return false;
        }
        String this$maskRule = this.getMaskRule();
        String other$maskRule = other.getMaskRule();
        if (this$maskRule == null ? other$maskRule != null : !this$maskRule.equals(other$maskRule)) {
            return false;
        }
        String this$level = this.getLevel();
        String other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
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
        return other instanceof SensitiveDataEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $datasourceId = this.getDatasourceId();
        result = result * 59 + ($datasourceId == null ? 43 : $datasourceId.hashCode());
        String $tableName = this.getTableName();
        result = result * 59 + ($tableName == null ? 43 : $tableName.hashCode());
        String $columnName = this.getColumnName();
        result = result * 59 + ($columnName == null ? 43 : $columnName.hashCode());
        String $sensitiveType = this.getSensitiveType();
        result = result * 59 + ($sensitiveType == null ? 43 : $sensitiveType.hashCode());
        String $maskRule = this.getMaskRule();
        result = result * 59 + ($maskRule == null ? 43 : $maskRule.hashCode());
        String $level = this.getLevel();
        result = result * 59 + ($level == null ? 43 : $level.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "SensitiveDataEntity(id=" + this.getId() + ", datasourceId=" + this.getDatasourceId() + ", tableName=" + this.getTableName() + ", columnName=" + this.getColumnName() + ", sensitiveType=" + this.getSensitiveType() + ", maskRule=" + this.getMaskRule() + ", level=" + this.getLevel() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

