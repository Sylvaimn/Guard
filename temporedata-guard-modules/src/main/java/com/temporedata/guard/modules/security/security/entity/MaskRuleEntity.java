/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.security.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_mask_rule")
public class MaskRuleEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=128)
    private String name;
    @Column(nullable=false, length=32)
    private String ruleType;
    @Column(length=128)
    private String maskPattern;
    @Column(length=512)
    private String description;
    @Column(length=32)
    private String datasourceId;
    @Column(length=64)
    private String tableName;
    @Column(length=64)
    private String columnName;
    @Column(nullable=false)
    private Integer status;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getRuleType() {
        return this.ruleType;
    }

    public String getMaskPattern() {
        return this.maskPattern;
    }

    public String getDescription() {
        return this.description;
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

    public Integer getStatus() {
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

    public void setName(String name) {
        this.name = name;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public void setMaskPattern(String maskPattern) {
        this.maskPattern = maskPattern;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setStatus(Integer status) {
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
        if (!(o instanceof MaskRuleEntity)) {
            return false;
        }
        MaskRuleEntity other = (MaskRuleEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
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
        String this$ruleType = this.getRuleType();
        String other$ruleType = other.getRuleType();
        if (this$ruleType == null ? other$ruleType != null : !this$ruleType.equals(other$ruleType)) {
            return false;
        }
        String this$maskPattern = this.getMaskPattern();
        String other$maskPattern = other.getMaskPattern();
        if (this$maskPattern == null ? other$maskPattern != null : !this$maskPattern.equals(other$maskPattern)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
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
        return other instanceof MaskRuleEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $ruleType = this.getRuleType();
        result = result * 59 + ($ruleType == null ? 43 : $ruleType.hashCode());
        String $maskPattern = this.getMaskPattern();
        result = result * 59 + ($maskPattern == null ? 43 : $maskPattern.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $datasourceId = this.getDatasourceId();
        result = result * 59 + ($datasourceId == null ? 43 : $datasourceId.hashCode());
        String $tableName = this.getTableName();
        result = result * 59 + ($tableName == null ? 43 : $tableName.hashCode());
        String $columnName = this.getColumnName();
        result = result * 59 + ($columnName == null ? 43 : $columnName.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "MaskRuleEntity(id=" + this.getId() + ", name=" + this.getName() + ", ruleType=" + this.getRuleType() + ", maskPattern=" + this.getMaskPattern() + ", description=" + this.getDescription() + ", datasourceId=" + this.getDatasourceId() + ", tableName=" + this.getTableName() + ", columnName=" + this.getColumnName() + ", status=" + this.getStatus() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

