package com.temporedata.guard.modules.dev.meta.entity;

import lombok.Data;
import javax.persistence.*;

/**
 * Auto-collected column metadata.
 */
@Data
@Entity
@Table(name = "temporedata_guard_meta_column")
public class MetaColumnEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String tableId;

    @Column(nullable = false, length = 128)
    private String columnName;

    @Column(nullable = false, length = 64)
    private String columnType;

    private Integer columnSize;

    private Integer decimalDigits;

    private Boolean nullable;

    @Column(length = 256)
    private String defaultValue;

    @Column(length = 512)
    private String comment;

    private Boolean primaryKey;

    private Integer ordinalPosition;
}