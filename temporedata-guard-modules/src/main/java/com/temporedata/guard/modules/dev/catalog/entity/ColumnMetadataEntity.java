package com.temporedata.guard.modules.dev.catalog.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Synced column metadata.
 */
@Data
@Entity
@Table(name = "temporedata_guard_column_metadata")
public class ColumnMetadataEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String tableMetadataId;

    @Column(nullable = false, length = 64)
    private String columnName;

    @Column(nullable = false, length = 32)
    private String dataType;

    private Integer columnSize;

    private Integer decimalDigits;

    @Column(length = 512)
    private String defaultValue;

    private Integer ordinalPosition;

    private Boolean isNullable;

    @Column(length = 512)
    private String comment;

    /** Comment value collected from the database at last sync; used to detect manual edits. */
    @Column(length = 512)
    private String syncedComment;

    private Boolean isPrimaryKey;

    @Column(length = 32)
    private String dataLevelId;
}