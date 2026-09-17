package com.temporedata.guard.modules.dev.query.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Ad-hoc query execution log.
 */
@Data
@Entity
@Table(name = "temporedata_guard_query_log")
public class QueryLogEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String datasourceId;

    @Column(nullable = false, length = 64)
    private String datasourceName;

    @Column(nullable = false)
    @Lob
    private String sql;

    @Column(length = 16)
    private String status; // SUCCESS, FAILED

    private long durationMs;

    @Column(length = 32)
    private String userId;

    @Column(length = 32)
    private String tenantId;

    @Column(length = 1024)
    private String errorMsg;

    @Column(nullable = false)
    private LocalDateTime createTime;
}