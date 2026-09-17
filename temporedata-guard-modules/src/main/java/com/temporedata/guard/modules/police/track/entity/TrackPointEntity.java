package com.temporedata.guard.modules.police.track.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 轨迹点（P1）：人员/车辆时空轨迹，支持活动规律与落脚点分析。
 */
@Data
@Entity
@Table(name = "temporedata_guard_track_point", indexes = {
        @javax.persistence.Index(name = "idx_tp_entity_time", columnList = "entityId, trackTime"),
        @javax.persistence.Index(name = "idx_tp_tenant", columnList = "tenantId")
})
public class TrackPointEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 轨迹对象(人员/车辆实体ID) */
    @Column(nullable = false, length = 32)
    private String entityId;

    @Column(nullable = false, length = 16)
    private String entityType = "PERSON";

    private Double lat;

    private Double lng;

    /** 场景: 卡口/小区/商场/酒店/高速口等 */
    @Column(length = 64)
    private String scene;

    /** 落脚点描述(聚合用) */
    @Column(length = 128)
    private String address;

    @Column(nullable = false)
    private LocalDateTime trackTime;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
