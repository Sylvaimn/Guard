package com.temporedata.guard.modules.dev.quality.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Data quality rule definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_quality_rule")
public class QualityRuleEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String datasourceId;

    @Column(nullable = false, length = 64)
    private String tableName;

    @Column(length = 64)
    private String columnName;

    @Column(nullable = false, length = 16)
    private String ruleType; // NOT_NULL, UNIQUE, RANGE, REGEX, CUSTOM_SQL

    @Column(length = 512)
    private String ruleConfig;

    @Column(length = 512)
    private String description;

    @Column(length = 16)
    private String lastStatus;

    @Column(nullable = false, length = 32)
    private String tenantId;

    /** 定时调度 cron(空=不调度) */
    private String scheduleCron;

    /** 规则状态: ACTIVE/INACTIVE */
    private String status = "ACTIVE";

    @Column(nullable = false)
    private LocalDateTime createTime;
}