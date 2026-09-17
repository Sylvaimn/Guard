package com.temporedata.guard.api.service;

import lombok.Data;

/**
 * Data API response.
 */
@Data
public class DataApiRes {

    private String id;

    private String name;

    private String description;

    private String datasourceId;

    private String sql;

    private String method; // GET, POST

    private String path;

    private String apiKey; // masked, only shown on create

    private String status; // ACTIVE, DISABLED

    private String cacheTtl;

    private Long callCount;

    private String lastCallTime;

    private String serviceTarget;

    private String dataScope;

    private String maskColumns;

    private Integer qpsLimit;

    private Integer dailyLimit;

    private String createTime;

    private String updateTime;
}