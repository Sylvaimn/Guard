package com.temporedata.guard.modules.dev.globalvar.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Global variables - reusable key-value pairs for workflow/script templates.
 */
@Data
@Entity
@Table(name = "temporedata_guard_global_var")
public class GlobalVarEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String varKey;

    @Column(nullable = false, length = 512)
    private String varValue;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}