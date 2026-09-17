package com.temporedata.guard.modules.platform.preference.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * User preference settings.
 */
@Data
@Entity
@Table(name = "temporedata_guard_preference")
public class PreferenceEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String userId;

    @Column(nullable = false, length = 64)
    private String prefKey;

    @Column(length = 1024)
    private String prefValue;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}