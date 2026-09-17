package com.temporedata.guard.modules.common.resource.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Resource center - unified resource management (files, jars, scripts, etc).
 */
@Data
@Entity
@Table(name = "temporedata_guard_resource")
public class ResourceEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 512)
    private String path;

    @Column(length = 32)
    private Long size;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 血缘关联表(文件入湖/导出目标表) */
    @Column(length = 128)
    private String tableId;

    /** 资源来源: UPLOAD(上传)/EXPORT(湖仓导出)/JOB(作业产物) */
    @Column(length = 16)
    private String sourceType = "UPLOAD";

    /** 是否参与血缘(默认参与) */
    @Column(nullable = false)
    private Boolean lineageable = true;
}