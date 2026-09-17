package com.temporedata.guard.modules.platform.passwordless.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Passwordless login configuration entity.
 */
@Data
@Entity
@Table(name = "temporedata_guard_passwordless_config")
public class PasswordlessConfigEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(length = 32)
    private String loginType;

    @Column(length = 4096)
    private String config;

    @Column(length = 16)
    private String status;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}