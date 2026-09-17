package com.temporedata.guard.api.datacenter;

import lombok.Data;

/**
 * Data center request DTO.
 */
@Data
public class DataCenterReq {

    private String name;

    private String description;

    private String category;

    private String config;

    private String datasourceId;

    private Integer refreshInterval;
}