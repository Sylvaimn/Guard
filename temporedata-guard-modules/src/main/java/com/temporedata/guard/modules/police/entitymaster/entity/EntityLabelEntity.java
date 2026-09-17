package com.temporedata.guard.modules.police.entitymaster.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 实体标签：身份/行为/时空/案件四类，带时效与置信度。
 */
@Data
@Entity
@Table(name = "temporedata_guard_entity_label", indexes = {
        @Index(name = "idx_el_entity", columnList = "entityId"),
        @Index(name = "idx_el_code_tenant", columnList = "labelCode, tenantId"),
        @Index(name = "idx_el_type_codes_tenant", columnList = "labelType, labelCode, tenantId")
    })
public class EntityLabelEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String entityId;

    /** 标签类型：IDENTITY / BEHAVIOR / SPATIO_TEMPORAL / CASE */
    @Column(nullable = false, length = 32)
    private String labelType;

    /** 标签编码，如 DRUG_HISTORY(吸毒前科)、FREQ_TRAVEL(跨省频繁) */
    @Column(nullable = false, length = 64)
    private String labelCode;

    /** 标签显示名 */
    @Column(length = 128)
    private String labelName;

    /** 标签来源 */
    @Column(length = 64)
    private String source;

    /** 置信度 0~1 */
    private Double confidence;

    /** 有效期至（null 表示长期） */
    private LocalDateTime expireTime;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
