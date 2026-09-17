package com.temporedata.guard.modules.dev.quality.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * A single quality check execution record.
 */
@Data
@Entity
@Table(name = "temporedata_guard_quality_check")
public class QualityCheckEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String ruleId;

    @Column(nullable = false, length = 16)
    private String status; // PASS, FAIL

    @Column(length = 1024)
    private String result;

    private long durationMs;

    @Column(length = 1024)
    private String errorMsg;

    @Column(nullable = false)
    private LocalDateTime checkTime;
}