package com.temporedata.guard.modules.common.cluster.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Cluster node definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_cluster_node")
public class ClusterNodeEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String clusterId;

    @Column(nullable = false, length = 128)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(length = 64)
    private String username;

    @Column(length = 512)
    private String password; // AES encrypted

    @Column(length = 16)
    private String agentStatus; // INSTALLING, ONLINE, OFFLINE

    @Column
    private Double cpuUsage;

    @Column
    private Double memoryUsage;

    @Column
    private Double diskUsage;

    @Column(nullable = false)
    private LocalDateTime createTime;
}