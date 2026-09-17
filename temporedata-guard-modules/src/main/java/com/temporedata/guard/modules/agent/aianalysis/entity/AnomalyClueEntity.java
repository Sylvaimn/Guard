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
@Table(name="temporedata_guard_anomaly_clue", indexes={@Index(name="idx_ac_tenant_status", columnList="tenantId, status"), @Index(name="idx_ac_target", columnList="target")})
public class AnomalyClueEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=32)
    private String metricType;
    @Column(nullable=false, length=128)
    private String target;
    private Double baseline;
    private Double current;
    private Double deviation;
    @Column(nullable=false, length=16)
    private String status = "OPEN";
    @Column(length=512)
    private String suggestedAction;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return this.id;
    }

    public String getMetricType() {
        return this.metricType;
    }

    public String getTarget() {
        return this.target;
    }

    public Double getBaseline() {
        return this.baseline;
    }

    public Double getCurrent() {
        return this.current;
    }

    public Double getDeviation() {
        return this.deviation;
    }

    public String getStatus() {
        return this.status;
    }

    public String getSuggestedAction() {
        return this.suggestedAction;
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

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setBaseline(Double baseline) {
        this.baseline = baseline;
    }

    public void setCurrent(Double current) {
        this.current = current;
    }

    public void setDeviation(Double deviation) {
        this.deviation = deviation;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
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
        if (!(o instanceof AnomalyClueEntity)) {
            return false;
        }
        AnomalyClueEntity other = (AnomalyClueEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$baseline = this.getBaseline();
        Double other$baseline = other.getBaseline();
        if (this$baseline == null ? other$baseline != null : !((Object)this$baseline).equals(other$baseline)) {
            return false;
        }
        Double this$current = this.getCurrent();
        Double other$current = other.getCurrent();
        if (this$current == null ? other$current != null : !((Object)this$current).equals(other$current)) {
            return false;
        }
        Double this$deviation = this.getDeviation();
        Double other$deviation = other.getDeviation();
        if (this$deviation == null ? other$deviation != null : !((Object)this$deviation).equals(other$deviation)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$metricType = this.getMetricType();
        String other$metricType = other.getMetricType();
        if (this$metricType == null ? other$metricType != null : !this$metricType.equals(other$metricType)) {
            return false;
        }
        String this$target = this.getTarget();
        String other$target = other.getTarget();
        if (this$target == null ? other$target != null : !this$target.equals(other$target)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$suggestedAction = this.getSuggestedAction();
        String other$suggestedAction = other.getSuggestedAction();
        if (this$suggestedAction == null ? other$suggestedAction != null : !this$suggestedAction.equals(other$suggestedAction)) {
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
        return other instanceof AnomalyClueEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $baseline = this.getBaseline();
        result = result * 59 + ($baseline == null ? 43 : ((Object)$baseline).hashCode());
        Double $current = this.getCurrent();
        result = result * 59 + ($current == null ? 43 : ((Object)$current).hashCode());
        Double $deviation = this.getDeviation();
        result = result * 59 + ($deviation == null ? 43 : ((Object)$deviation).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $metricType = this.getMetricType();
        result = result * 59 + ($metricType == null ? 43 : $metricType.hashCode());
        String $target = this.getTarget();
        result = result * 59 + ($target == null ? 43 : $target.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $suggestedAction = this.getSuggestedAction();
        result = result * 59 + ($suggestedAction == null ? 43 : $suggestedAction.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "AnomalyClueEntity(id=" + this.getId() + ", metricType=" + this.getMetricType() + ", target=" + this.getTarget() + ", baseline=" + this.getBaseline() + ", current=" + this.getCurrent() + ", deviation=" + this.getDeviation() + ", status=" + this.getStatus() + ", suggestedAction=" + this.getSuggestedAction() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

