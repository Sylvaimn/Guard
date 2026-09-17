package com.temporedata.guard.modules.police.network.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 关系网络统一边表（P1）：资金/通联/同案/共址 四类关系统一建模。
 */
@Data
@Entity
@Table(name = "temporedata_guard_graph_edge", indexes = {
        @javax.persistence.Index(name = "idx_ge_source", columnList = "sourceEntityId"),
        @javax.persistence.Index(name = "idx_ge_target", columnList = "targetEntityId"),
        @javax.persistence.Index(name = "idx_ge_type", columnList = "relationType"),
        @javax.persistence.Index(name = "idx_ge_tenant", columnList = "tenantId")
})
public class GraphEdgeEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 源实体 */
    @Column(nullable = false, length = 32)
    private String sourceEntityId;

    @Column(nullable = false, length = 32)
    private String targetEntityId;

    /** FUND(资金)/CONTACT(通联)/SAME_CASE(同案)/CO_LOCATE(共址) */
    @Column(nullable = false, length = 16)
    private String relationType;

    /** 权重(资金金额/通联频次/共址次数) */
    private Double weight;

    /** 时间窗(最近一次) */
    private LocalDateTime timeWindow;

    /** 证据引用(来源记录/案件) */
    @Column(length = 256)
    private String evidence;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
