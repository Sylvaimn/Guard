package com.temporedata.guard.modules.platform.secret.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Secret key-value management for secure credential storage.
 */
@Data
@Entity
@Table(name = "temporedata_guard_secret")
public class SecretEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String key;

    @Column(nullable = false, length = 1024)
    private String value;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String scope; // GLOBAL, WORKFLOW

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}