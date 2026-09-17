package com.temporedata.guard.modules.dev.lineage.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Graph edge: FLOWS_TO / DERIVED_FROM / PRODUCES / CONSUMES / USES / INCLUDES.
 */
@Data
@Entity
@Table(name = "temporedata_guard_lineage_edge")
public class LineageEdgeEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String sourceId;

    @Column(nullable = false, length = 64)
    private String targetId;

    /** FLOWS_TO, DERIVED_FROM, PRODUCES, CONSUMES, USES, INCLUDES */
    @Column(nullable = false, length = 16)
    private String edgeType;

    @Column(length = 64)
    private String taskId;

    @Column(length = 128)
    private String taskName;

@Column(columnDefinition = "TEXT", name = "`sql`")
    private String sql;

    @Column(length = 512)
    private String transform;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;
}