package com.temporedata.guard.modules.police.entitymaster.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 实体来源映射：EntityID 与各来源系统的原始记录绑定（含置信度）。
 */
@Data
@Entity
@Table(name = "temporedata_guard_entity_mapping", indexes = {
        @Index(name = "idx_emap_entity", columnList = "entityId"),
        @Index(name = "idx_emap_record", columnList = "sourceRecordId")
    })
public class EntityMappingEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 实体主数据 ID */
    @Column(nullable = false, length = 32)
    private String entityId;

    /** 来源系统标识（警综/卡口/话单...） */
    @Column(nullable = false, length = 32)
    private String sourceSystem;

    /** 来源系统原始记录 ID */
    @Column(nullable = false, length = 64)
    private String sourceRecordId;

    /** 匹配置信度 0~1 */
    private Double confidence;

    /** 匹配方式：RULE(规则强匹配) / SIMILAR(相似聚类) / MANUAL(人工确认) */
    @Column(nullable = false, length = 16)
    private String matchMethod = "RULE";

    @Column(nullable = false)
    private LocalDateTime createTime;
}
