package com.temporedata.guard.modules.platform.tenant.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Tenant - the top-level isolation unit of the platform.
 */
@Data
@Entity
@Table(name = "temporedata_guard_tenant")
public class TenantEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String code;

    @Column(length = 64)
    private String contactName;

    @Column(length = 128)
    private String contactEmail;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createTime;
}