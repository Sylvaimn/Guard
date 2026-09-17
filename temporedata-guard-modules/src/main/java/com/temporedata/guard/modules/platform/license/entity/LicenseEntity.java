package com.temporedata.guard.modules.platform.license.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Platform license information.
 */
@Data
@Entity
@Table(name = "temporedata_guard_license")
public class LicenseEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 512)
    private String licenseKey;

    @Column(nullable = false, length = 16)
    private String status; // ACTIVE, EXPIRED, INVALID

    @Column
    private LocalDateTime expireDate;

    @Column
    private LocalDateTime installDate;

    @Column(nullable = false, length = 32)
    private String tenantId;
}