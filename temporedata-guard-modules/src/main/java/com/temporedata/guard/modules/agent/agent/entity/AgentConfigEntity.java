package com.temporedata.guard.modules.agent.agent.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Agent LLM API configuration.
 */
@Data
@Entity
@Table(name = "temporedata_guard_agent_config")
public class AgentConfigEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(length = 32)
    private String provider;

    @Column(length = 255)
    private String baseUrl;

    @Column(length = 512)
    private String apiKey;

    @Column(length = 128)
    private String model;

    private Double temperature;

    private Boolean enabled;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}