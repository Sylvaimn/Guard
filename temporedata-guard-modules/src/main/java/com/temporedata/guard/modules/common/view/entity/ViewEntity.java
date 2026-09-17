package com.temporedata.guard.modules.common.view.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Data view management entity.
 * Supports creating named views from SQL queries for data visualization.
 */
@Data
@Entity
@Table(name = "temporedata_guard_view")
public class ViewEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String remark;

    @Column(length = 32)
    private String status; // DRAFT, PUBLISHED

    @Column(length = 32)
    private String datasourceId;

    @Column(columnDefinition = "TEXT")
    private String querySql; // SQL query backing this view

    @Column(length = 32)
    private String backgroundFileId;

    @Column(columnDefinition = "TEXT")
    private String webConfig; // JSON config for web display

    @Column(columnDefinition = "TEXT")
    private String cardList; // JSON array of dashboard cards

    @Column(nullable = false, length = 32)
    private String tenantId;

    private LocalDateTime createDateTime;

    private LocalDateTime lastModifiedDateTime;

    @Column(length = 32)
    private String createBy;

    @Column(length = 32)
    private String lastModifiedBy;
}