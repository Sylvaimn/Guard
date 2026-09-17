package com.temporedata.guard.api.meta;

import lombok.Data;

/**
 * Request to sync metadata from a datasource.
 */
@Data
public class MetaSyncReq {

    private String datasourceId;
    private String schemaName;
    private String tableName;
}