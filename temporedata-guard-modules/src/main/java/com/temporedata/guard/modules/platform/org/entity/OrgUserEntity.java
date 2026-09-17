package com.temporedata.guard.modules.platform.org.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Department member association - binds users to departments.
 */
@Data
@Entity
@Table(name = "temporedata_guard_org_user")
public class OrgUserEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String orgId;

    @Column(nullable = false, length = 32)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
