package com.temporedata.guard.api.entitymaster;

import lombok.Data;

@Data
public class EntityMappingRes {

    private String id;

    private String entityId;

    private String sourceSystem;

    private String sourceRecordId;

    private Double confidence;

    private String matchMethod;

    private String createTime;
}
