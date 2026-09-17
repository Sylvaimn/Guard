package com.temporedata.guard.modules.dev.datacenter.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Data center dashboard entity.
 */
@Data
@Entity
@Table(name = "temporedata_guard_data_center")
public class DataCenterEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 64)
    private String category;

    @Column(length = 8192)
    private String config;

    @Column(length = 32)
    private String datasourceId;

    private Integer refreshInterval;

    @Column(length = 16)
    private String status;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}