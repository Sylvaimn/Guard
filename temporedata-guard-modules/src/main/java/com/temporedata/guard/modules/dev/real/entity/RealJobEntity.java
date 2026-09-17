package com.temporedata.guard.modules.dev.real.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Realtime (streaming) job definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_real_job")
public class RealJobEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String type; // FLINK_SQL, FLINK_JAR

    @Lob
    @Column
    private String script;

    @Column(length = 32)
    private String clusterId;

    @Column(nullable = false, length = 16)
    private String status; // STOPPED, RUNNING, FAILED

    @Column(length = 512)
    private String checkpointPath;

    @Column(length = 512)
    private String savepointPath;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}