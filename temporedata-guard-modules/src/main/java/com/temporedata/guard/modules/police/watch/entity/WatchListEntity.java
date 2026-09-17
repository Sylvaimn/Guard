package com.temporedata.guard.modules.police.watch.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 布控名单（警用）：以归一化实体为对象，支持分组/级别/有效期/密级。
 */
@Data
@Entity
@Table(name = "temporedata_guard_watch_list", indexes = {
        @Index(name = "idx_wl_entity", columnList = "entityId"),
        @Index(name = "idx_wl_tenant_status", columnList = "tenantId, status")
    })
public class WatchListEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 布控对象（归一化实体 ID，复用 entity-master） */
    @Column(nullable = false, length = 32)
    private String entityId;

    /** 实体类型：PERSON / VEHICLE / CASE / ITEM */
    @Column(nullable = false, length = 16)
    private String entityType;

    /** 对象显示名（姓名/车牌等） */
    @Column(length = 128)
    private String targetName;

    /** 名单分组：部级 / 省级 / 市级 / 临时 */
    @Column(nullable = false, length = 16)
    private String listGroup = "市级";

    /** 布控级别：1最高 */
    @Column(nullable = false)
    private Integer level = 2;

    /** 布控原因 */
    @Column(length = 512)
    private String reason;

    /** 数据密级 */
    @Column(nullable = false, length = 32)
    private String density = "SECRET";

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    /** 状态：ACTIVE / EXPIRED / REVOKED */
    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
