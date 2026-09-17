package com.temporedata.guard.api.service;

import lombok.Data;

/**
 * Data API publishing request.
 */
@Data
public class DataApiReq {

    private String name;

    private String description;

    private String datasourceId;

    private String sql; // SQL template, supports ${param} placeholders

    private String method; // GET, POST

    private String path; // unique API path, e.g. /data/users

    private String cacheTtl; // cache TTL in seconds, 0 = no cache

    /** 服务对象(INTERNAL内部系统/SUPERIOR上级机关/APP警务通终端/CASE专案组) */
    private String serviceTarget;

    /** 数据范围(UNIT本单位/DEPT本部门/CASE专案内/GLOBAL全局) */
    private String dataScope;

    /** 脱敏列(逗号分隔, 调用方等级不足时打码) */
    private String maskColumns;

    /** QPS 上限 */
    private Integer qpsLimit;

    /** 每日调用上限 */
    private Integer dailyLimit;
}