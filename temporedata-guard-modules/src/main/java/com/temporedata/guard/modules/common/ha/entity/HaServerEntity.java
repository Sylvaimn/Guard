package com.temporedata.guard.modules.common.ha.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * High availability server registration.
 * Tracks active server instances for HA coordination.
 */
@Data
@Entity
@Table(name = "temporedata_guard_ha_server")
public class HaServerEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String serverName;

    @Column(nullable = false, length = 128)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(length = 32)
    private String status; // ACTIVE, STANDBY, OFFLINE

    private LocalDateTime lastHeartbeat;

    private LocalDateTime registerTime;

    @Column(nullable = false, length = 32)
    private String tenantId;
}