package com.temporedata.guard.modules.dev.meta.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Field-level (column-to-column) lineage relationships.
 * Links specific source columns to target columns within table-level lineage.
 */
@Data
@Entity
@Table(name = "temporedata_guard_meta_field_lineage")
public class MetaFieldLineageEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String lineageId; // FK to temporedata_guard_meta_lineage

    @Column(nullable = false, length = 32)
    private String sourceTableId;

    @Column(nullable = false, length = 128)
    private String sourceColumnName;

    @Column(nullable = false, length = 32)
    private String targetTableId;

    @Column(nullable = false, length = 128)
    private String targetColumnName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String transformationLogic; // e.g. "UPPER(source.name)", "source.amount * 1.1"

    @Column(length = 256)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createTime;
}