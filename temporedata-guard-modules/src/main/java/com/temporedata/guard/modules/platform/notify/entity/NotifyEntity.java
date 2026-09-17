package com.temporedata.guard.modules.platform.notify.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Notification configuration - email, SMS, webhook channels.
 */
@Data
@Entity
@Table(name = "temporedata_guard_notify")
public class NotifyEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String channelType;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 1024)
    private String config;

    @Column(nullable = false)
    private Integer enabled = 0;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}