package com.temporedata.guard.modules.common.container.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Container management entity.
 * Tracks Docker/K8s container instances for job execution.
 */
@Data
@Entity
@Table(name = "temporedata_guard_container")
public class ContainerEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String type; // DOCKER, K8S

    @Column(length = 256)
    private String image;

    @Column(length = 128)
    private String containerId; // External container ID

    @Column(length = 32)
    private String status; // CREATED, RUNNING, STOPPED, FAILED

    @Column(length = 32)
    private String clusterId;

    @Column(length = 32)
    private String clusterNodeId;

    private Integer cpuCores;

    private Integer memoryMb;

    @Column(length = 512)
    private String envVars; // JSON string of environment variables

    @Column(length = 512)
    private String portMappings; // JSON string of port mappings

    @Column(length = 512)
    private String volumeMounts; // JSON string of volume mounts

    @Column(length = 1024)
    private String command;

    @Column(columnDefinition = "TEXT")
    private String log;

    @Column(nullable = false, length = 32)
    private String tenantId;

    private LocalDateTime createDateTime;

    private LocalDateTime lastModifiedDateTime;
}