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
@Table(name="temporedata_guard_ai_rule", indexes={@Index(name="idx_ar_tenant_status", columnList="tenantId, status")})
public class AiRuleEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(nullable=false, length=512)
    private String nluText;
    @Column(nullable=false, length=2000)
    private String ruleJson;
    @Column(nullable=false, length=16)
    private String status = "DRAFT";
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false, length=32)
    private String creatorId;
    private Integer hitCount = 0;
    @Column(nullable=false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return this.id;
    }

    public String getNluText() {
        return this.nluText;
    }

    public String getRuleJson() {
        return this.ruleJson;
    }

    public String getStatus() {
        return this.status;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getCreatorId() {
        return this.creatorId;
    }

    public Integer getHitCount() {
        return this.hitCount;
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

    public void setNluText(String nluText) {
        this.nluText = nluText;
    }

    public void setRuleJson(String ruleJson) {
        this.ruleJson = ruleJson;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
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
        if (!(o instanceof AiRuleEntity)) {
            return false;
        }
        AiRuleEntity other = (AiRuleEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$hitCount = this.getHitCount();
        Integer other$hitCount = other.getHitCount();
        if (this$hitCount == null ? other$hitCount != null : !((Object)this$hitCount).equals(other$hitCount)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$nluText = this.getNluText();
        String other$nluText = other.getNluText();
        if (this$nluText == null ? other$nluText != null : !this$nluText.equals(other$nluText)) {
            return false;
        }
        String this$ruleJson = this.getRuleJson();
        String other$ruleJson = other.getRuleJson();
        if (this$ruleJson == null ? other$ruleJson != null : !this$ruleJson.equals(other$ruleJson)) {
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
        String this$creatorId = this.getCreatorId();
        String other$creatorId = other.getCreatorId();
        if (this$creatorId == null ? other$creatorId != null : !this$creatorId.equals(other$creatorId)) {
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
        return other instanceof AiRuleEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $hitCount = this.getHitCount();
        result = result * 59 + ($hitCount == null ? 43 : ((Object)$hitCount).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $nluText = this.getNluText();
        result = result * 59 + ($nluText == null ? 43 : $nluText.hashCode());
        String $ruleJson = this.getRuleJson();
        result = result * 59 + ($ruleJson == null ? 43 : $ruleJson.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        String $creatorId = this.getCreatorId();
        result = result * 59 + ($creatorId == null ? 43 : $creatorId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "AiRuleEntity(id=" + this.getId() + ", nluText=" + this.getNluText() + ", ruleJson=" + this.getRuleJson() + ", status=" + this.getStatus() + ", tenantId=" + this.getTenantId() + ", creatorId=" + this.getCreatorId() + ", hitCount=" + this.getHitCount() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

