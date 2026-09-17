package com.temporedata.guard.modules.police.watch.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 预警记录（警用）：命中布控名单生成的预警，含签收/处置/战果闭环。
 */
@Data
@Entity
@Table(name = "temporedata_guard_watch_alert", indexes = {
        @Index(name = "idx_wa_tenant_status", columnList = "tenantId, status"),
        @Index(name = "idx_wa_list", columnList = "watchListId"),
        @Index(name = "idx_wa_entity", columnList = "entityId")
    })
public class WatchAlertEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 命中的布控名单 */
    @Column(nullable = false, length = 32)
    private String watchListId;

    /** 命中对象实体 */
    @Column(nullable = false, length = 32)
    private String entityId;

    /** 命中对象显示名 */
    @Column(length = 128)
    private String targetName;

    /** 命中场景/规则描述（如：卡口过车、实名核验） */
    @Column(length = 512)
    private String hitScene;

    /** 来源事件（如卡口号、核验点） */
    @Column(length = 256)
    private String hitSource;

    /** 预警级别（继承名单级别） */
    @Column(nullable = false)
    private Integer level = 2;

    /** 数据密级 */
    @Column(nullable = false, length = 32)
    private String density = "SECRET";

    /** 状态：PENDING(待签收) / PROCESSING(处置中) / CLOSED(已办结) */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    /** 签收人（警员） */
    @Column(length = 64)
    private String assignee;

    /** 处置反馈 */
    @Column(length = 1024)
    private String feedback;

    /** 处置结果：CAPTURED(抓获) / EXCLUDED(排除) / PENDING(待查) */
    @Column(length = 16)
    private String result;

    /** 战果描述（抓获归因/破案贡献） */
    @Column(length = 512)
    private String merit;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime assignTime;

    private LocalDateTime closeTime;
}
