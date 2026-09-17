package com.temporedata.guard.modules.common.driver.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * JDBC driver management for database connections.
 */
@Data
@Entity
@Table(name = "temporedata_guard_driver")
public class DriverEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String dbType;

    @Column(nullable = false, length = 64)
    private String version;

    @Column(length = 512)
    private String driverPath;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}