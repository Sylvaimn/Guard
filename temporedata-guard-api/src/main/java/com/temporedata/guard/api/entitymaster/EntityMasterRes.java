package com.temporedata.guard.api.entitymaster;

import lombok.Data;

import java.util.List;

/**
 * 实体主数据 + 映射 + 标签 聚合视图。
 */
@Data
public class EntityMasterRes {

    private String id;

    private String entityType;

    private String primaryKey;

    private String displayName;

    private String density;

    private String status;

    private Double confidence;

    private List<EntityMappingRes> mappings;

    private List<EntityLabelRes> labels;

    private String createTime;
}
