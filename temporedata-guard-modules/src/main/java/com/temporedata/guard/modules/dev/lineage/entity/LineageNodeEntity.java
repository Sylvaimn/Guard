package com.temporedata.guard.modules.dev.lineage.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Graph node: TABLE / COLUMN / TASK / REPORT / DATA_SOURCE.
 * One graph space per tenant (physical isolation via tenant_id).
 */
@Data
@Entity
@Table(name = "temporedata_guard_lineage_node")
public class LineageNodeEntity {

    @Id
    @Column(length = 64)
    private String id;

    /** TABLE, COLUMN, TASK, REPORT, DATA_SOURCE */
    @Column(nullable = false, length = 16)
    private String nodeType;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String db;

@Column(length = 64, name = "`schema`")
    private String schema;

    @Column(length = 32)
    private String projectId;

    @Column(length = 128)
    private String owner;

    /** extra properties as JSON: datasourceName, rowCount, columnCount, ruleType ... */
    @Column(columnDefinition = "TEXT")
    private String properties;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}