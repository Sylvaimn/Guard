package com.temporedata.guard.modules.platform.org.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Organization structure - department tree.
 */
@Data
@Entity
@Table(name = "temporedata_guard_org")
public class OrgEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String parentId;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(length = 32)
    private String responsibleUserId;

    @Column(length = 32)
    private String defaultRoleId;

    /** 部门编号(警务机构编码) */
    private String orgCode;

    /** 警种(刑侦/治安/网安/经侦/禁毒/技侦/情报/综合) */
    private String policeType;

    /** 编制人数 */
    private Integer headcount;

    /** 部门类型(ORG机关/UNIT警种部门/STATION派出所/CASE专案组) */
    private String orgType;

    /** 专案组到期时间(临时建制) */
    private String expireTime;

    @Column(nullable = false)
    private LocalDateTime createTime;
}