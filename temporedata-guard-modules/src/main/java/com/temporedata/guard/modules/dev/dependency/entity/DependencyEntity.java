package com.temporedata.guard.modules.dev.dependency.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Dependency collection - groups of reusable task dependencies.
 */
@Data
@Entity
@Table(name = "temporedata_guard_dependency")
public class DependencyEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 2048)
    private String config;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}