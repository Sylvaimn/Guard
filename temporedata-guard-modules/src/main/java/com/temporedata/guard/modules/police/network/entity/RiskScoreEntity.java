package com.temporedata.guard.modules.police.network.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 重点人员管控积分（P2）：多维指标加权 → 风险等级(红/黄/橙) → 预警联动。
 */
@Data
@Entity
@Table(name = "temporedata_guard_risk_score", indexes = {
        @javax.persistence.Index(name = "idx_rs_entity", columnList = "entityId"),
        @javax.persistence.Index(name = "idx_rs_level", columnList = "riskLevel"),
        @javax.persistence.Index(name = "idx_rs_tenant", columnList = "tenantId")
})
public class RiskScoreEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String entityId;

    @Column(nullable = false, length = 64)
    private String entityName;

    /** 0-100 风险积分 */
    private Integer score;

    /** RED / YELLOW / ORANGE / LOW */
    @Column(nullable = false, length = 16)
    private String riskLevel = "LOW";

    /** 指标明细(JSON: {traffic, contact, gather, fund, watch}) */
    @Column(length = 1000)
    private String factors;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
