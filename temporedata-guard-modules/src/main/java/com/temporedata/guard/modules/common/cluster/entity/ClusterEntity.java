package com.temporedata.guard.modules.common.cluster.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Cluster management definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_cluster")
public class ClusterEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String type; // SPARK_STANDALONE, YARN, K8S

    @Column(length = 256)
    private String masterUrl;

    @Column(length = 16)
    private String status; // ONLINE, OFFLINE, INSTALLING

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}