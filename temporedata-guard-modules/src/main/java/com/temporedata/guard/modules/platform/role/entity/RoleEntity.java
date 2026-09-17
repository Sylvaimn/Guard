package com.temporedata.guard.modules.platform.role.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Role entity for RBAC. Roles are platform-wide (e.g. ADMIN / DEVELOPER).
 */
@Data
@Entity
@Table(name = "temporedata_guard_role")
public class RoleEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Boolean protectedRole = false;

    /** 数据范围(GLOBAL全局/UNIT本单位/DEPT本部门/CASE专案内) */
    private String dataScope = "UNIT";

    @Column(nullable = false)
    private LocalDateTime createTime;
}