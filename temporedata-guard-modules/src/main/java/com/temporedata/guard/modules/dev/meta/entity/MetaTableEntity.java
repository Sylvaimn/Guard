package com.temporedata.guard.modules.dev.meta.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Auto-collected table metadata from datasources.
 */
@Data
@Entity
@Table(name = "temporedata_guard_meta_table")
public class MetaTableEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String datasourceId;

    @Column(length = 64)
    private String schemaName;

    @Column(nullable = false, length = 128)
    private String tableName;

    @Column(length = 512)
    private String comment;

    private Long rowCount;

    private Long dataSize;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime syncTime;

    private LocalDateTime createTime;
}