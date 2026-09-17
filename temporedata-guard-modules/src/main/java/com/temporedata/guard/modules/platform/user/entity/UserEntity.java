package com.temporedata.guard.modules.platform.user.entity;

import com.temporedata.guard.modules.platform.role.entity.RoleEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity. Each user belongs to exactly one tenant and may hold many roles.
 */
@Data
@Entity
@Table(name = "temporedata_guard_user")
public class UserEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column
    private Integer status;

    /** 人员密级：PUBLIC / INTERNAL / CONFIDENTIAL / SECRET（数据安全法分级语义） */
    @Column(nullable = false, length = 32)
    private String density = "INTERNAL";

    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 首次登录/管理员重置后需强制改密(允许 NULL 兼容存量数据, 逻辑按 FALSE 处理) */
    @Column
    private Boolean mustChangePassword = false;

    @ManyToMany
    @JoinTable(name = "temporedata_guard_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    public Set<String> getRoleCodes() {
        Set<String> codes = new HashSet<>();
        if (roles != null) {
            roles.forEach(r -> codes.add(r.getCode()));
        }
        return codes;
    }
}