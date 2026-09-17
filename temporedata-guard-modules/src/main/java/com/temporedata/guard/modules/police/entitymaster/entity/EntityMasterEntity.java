package com.temporedata.guard.modules.police.entitymaster.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 实体主数据（警用归一化核心）：
 * 多源人员/物品/案件记录经归一化后统一挂到一个 EntityID 下。
 */
@Data
@Entity
@Table(name = "temporedata_guard_entity_master", indexes = {
        @Index(name = "idx_em_tenant_type", columnList = "tenantId, entityType"),
        @Index(name = "idx_em_tenant_status", columnList = "tenantId, status"),
        @Index(name = "idx_em_pk_type", columnList = "primaryKey, entityType"),
        @Index(name = "idx_em_density", columnList = "density")
    })
public class EntityMasterEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 实体类型：PERSON / CASE / ITEM / ORG */
    @Column(nullable = false, length = 16)
    private String entityType;

    /** 主证件/主标识（如身份证号、车牌号、案件编号） */
    @Column(length = 64)
    private String primaryKey;

    /** 实体显示名（人员姓名/物品名等） */
    @Column(length = 128)
    private String displayName;

    /** 数据密级：PUBLIC/INTERNAL/CONFIDENTIAL/SECRET */
    @Column(nullable = false, length = 32)
    private String density = "INTERNAL";

    /** 归一化状态：MASTER(已确认) / CANDIDATE(候选待确认) */
    @Column(nullable = false, length = 16)
    private String status = "CANDIDATE";

    /** 关联度评分（候选时才有效） */
    private Double confidence;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
