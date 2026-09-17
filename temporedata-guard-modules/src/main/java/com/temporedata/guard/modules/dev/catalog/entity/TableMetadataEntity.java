package com.temporedata.guard.modules.dev.catalog.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Synced table metadata from datasource.
 */
@Data
@Entity
@Table(name = "temporedata_guard_table_metadata")
public class TableMetadataEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String datasourceId;

    @Column(length = 64)
    private String schemaName;

    @Column(nullable = false, length = 64)
    private String tableName;

    @Column(length = 512)
    private String comment;

    /** Comment value collected from the database at last sync; used to detect manual edits. */
    @Column(length = 512)
    private String syncedComment;

    private Long rowCount;

    @Column(length = 32)
    private String dataLevelId;

    @Column(length = 32)
    private String dataCategoryId;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime syncTime;
}