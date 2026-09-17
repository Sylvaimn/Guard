package com.temporedata.guard.api.sensitive;

import lombok.Data;

/**
 * Sensitive data request DTO.
 */
@Data
public class SensitiveDataReq {

    private String datasourceId;

    private String tableName;

    private String columnName;

    private String sensitiveType;

    private String maskRule;

    private String level;

    private String description;
}