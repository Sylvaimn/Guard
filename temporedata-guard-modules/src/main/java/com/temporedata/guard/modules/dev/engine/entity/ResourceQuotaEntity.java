package com.temporedata.guard.modules.dev.engine.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 租户资源配额: 限制引擎作业 Executor 数/内存/执行时长/并发数。
 */
@Data
@Entity
@Table(name = "temporedata_guard_resource_quota")
public class ResourceQuotaEntity {

    @Id
    @Column(length = 32)
    private String tenantId;

    @Column(nullable = false)
    private Integer maxExecutors = 4;

    @Column(nullable = false)
    private Integer maxMemoryGb = 8;

    @Column(nullable = false)
    private Integer maxRuntimeMin = 60;

    @Column(nullable = false)
    private Integer maxConcurrency = 2;

    private LocalDateTime updateTime;
}
