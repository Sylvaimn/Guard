package com.temporedata.guard.modules.dev.meta.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistent table-level lineage relationships.
 * Supports multi-level lineage traversal via source_table_id -> target_table_id.
 */
@Data
@Entity
@Table(name = "temporedata_guard_meta_lineage")
public class MetaLineageEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String sourceTableId;

    @Column(nullable = false, length = 128)
    private String sourceTableName;

    @Column(nullable = false, length = 32)
    private String targetTableId;

    @Column(nullable = false, length = 128)
    private String targetTableName;

    @Column(nullable = false, length = 32)
    private String relationType; // SYNC, INGESTION, WORKFLOW, QUERY, MANUAL

    @Column(length = 128)
    private String relationName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String transformationSql; // SQL transformation logic

    @Column(length = 512)
    private String description; // human-readable description of the lineage

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}