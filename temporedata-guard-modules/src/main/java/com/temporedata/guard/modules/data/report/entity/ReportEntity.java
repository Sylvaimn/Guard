package com.temporedata.guard.modules.data.report.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Data report definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_report")
public class ReportEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 32)
    private String type; // TABLE, CHART, DASHBOARD

    @Column(length = 8192)
    private String config; // JSON config

    @Column(length = 32)
    private String datasourceId;

    @Column(length = 32)
    private String status; // DRAFT, PUBLISHED

    private String secretLevel;

    private String caliberDesc;

    private String schedulePeriod;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}