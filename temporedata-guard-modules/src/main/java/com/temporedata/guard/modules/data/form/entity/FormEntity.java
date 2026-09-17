package com.temporedata.guard.modules.data.form.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Form definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_form")
public class FormEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 8192)
    private String config; // JSON form schema

    @Column(length = 32)
    private String status; // DRAFT, PUBLISHED, CLOSED

    @Column(length = 64)
    private String shareToken;

    private String category;

    private Boolean sensitive;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}