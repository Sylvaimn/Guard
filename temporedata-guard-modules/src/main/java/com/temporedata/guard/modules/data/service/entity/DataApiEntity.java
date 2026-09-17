package com.temporedata.guard.modules.data.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Published data API - exposes SQL queries as REST endpoints.
 */
@Data
@Entity
@Table(name = "temporedata_guard_data_api")
public class DataApiEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 32)
    private String datasourceId;

    @Column(name = "`sql`", nullable = false, length = 4096)
    private String sql;

    @Column(nullable = false, length = 8)
    private String method; // GET, POST

    @Column(nullable = false, length = 128, unique = true)
    private String path;

    @Column(nullable = false, length = 64)
    private String apiKey; // auto-generated on create

    @Column(nullable = false, length = 16)
    private String status; // ACTIVE, DISABLED

    @Column(length = 16)
    private String cacheTtl;

    private Long callCount;

    private String serviceTarget;

    private String dataScope;

    private String maskColumns;

    private Integer qpsLimit;

    private Integer dailyLimit;

    /** 当日调用日期(YYYY-MM-DD) */
    private String todayDate;

    /** 当日调用次数 */
    private Long todayCalls;

    private LocalDateTime lastCallTime;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}