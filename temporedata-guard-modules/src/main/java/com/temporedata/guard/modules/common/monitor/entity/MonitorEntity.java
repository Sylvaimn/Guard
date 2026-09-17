package com.temporedata.guard.modules.common.monitor.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * System and node monitoring metrics.
 * Collected periodically via scheduled task.
 */
@Data
@Entity
@Table(name = "temporedata_guard_monitor")
public class MonitorEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(length = 32)
    private String clusterId;

    @Column(length = 32)
    private String clusterNodeId;

    @Column(length = 32)
    private String status; // SUCCESS, FAIL

    @Column(columnDefinition = "TEXT")
    private String log;

    private Double cpuPercent;

    private Double usedMemorySize; // GB

    private Double usedStorageSize; // GB

    private Double networkIoReadSpeed; // KB/s

    private Double networkIoWriteSpeed; // KB/s

    private Double diskIoReadSpeed; // KB/s

    private Double diskIoWriteSpeed; // KB/s

    @Column(nullable = false, length = 32)
    private String tenantId;

    private LocalDateTime createDateTime;
}