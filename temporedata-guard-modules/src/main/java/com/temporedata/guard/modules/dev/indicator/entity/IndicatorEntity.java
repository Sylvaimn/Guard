package com.temporedata.guard.modules.dev.indicator.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Data indicator definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_indicator")
public class IndicatorEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64, unique = true)
    private String code;

    @Column(length = 32)
    private String type; // COUNT, SUM, AVG, RATIO, CUSTOM

    @Column(length = 512)
    private String description;

    @Column(length = 4096)
    private String querySql;

    @Column(length = 32)
    private String datasourceId;

    @Column(length = 32)
    private String unit;

    @Column(length = 32)
    private String version;

    @Column(length = 64)
    private String theme;

    @Column(length = 64)
    private String owner;

    @Column(length = 16)
    private String status = "ENABLED"; // ENABLED / DISABLED

    @Column(length = 64)
    private String scheduleCron;

    private Boolean scheduleEnabled = false;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}