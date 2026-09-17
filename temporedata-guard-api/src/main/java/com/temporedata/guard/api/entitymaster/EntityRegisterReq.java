package com.temporedata.guard.api.entitymaster;

import lombok.Data;

/**
 * 归一化请求：将一条来源记录注册到实体主数据。
 */
@Data
public class EntityRegisterReq {

    private String entityType;      // PERSON / CASE / ITEM / ORG

    private String primaryKey;      // 主证件/主标识

    private String displayName;

    private String density = "INTERNAL";

    private String sourceSystem;

    private String sourceRecordId;

    private String matchMethod = "RULE";
}
