package com.temporedata.guard.modules.dev.indicator.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Indicator execution history record.
 */
@Data
@Entity
@Table(name = "temporedata_guard_indicator_run")
public class IndicatorRunEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "indicator_id", length = 32)
    private String indicatorId;

    @Column(name = "result_value", length = 255)
    private String resultValue;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "error_msg", length = 1000)
    private String errorMsg;

    @Column(name = "execute_time")
    private LocalDateTime executeTime;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;
}
