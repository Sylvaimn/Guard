package com.temporedata.guard.modules.dev.tag.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Tag classification (category container for tags).
 */
@Data
@Entity
@Table(name = "temporedata_guard_classification")
public class ClassificationEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32, unique = true)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}