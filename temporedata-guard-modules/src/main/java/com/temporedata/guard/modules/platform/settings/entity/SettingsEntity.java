package com.temporedata.guard.modules.platform.settings.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Platform-wide settings (key-value pairs).
 */
@Data
@Entity
@Table(name = "temporedata_guard_settings")
public class SettingsEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String configKey;

    @Column(length = 1024)
    private String configValue;

    @Column(length = 255)
    private String description;

    @Column(length = 32)
    private String groupName;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}