package com.temporedata.guard.api.intelligence;

import lombok.Data;

import java.util.List;

/**
 * 实体情报检索结果（含脱敏）。
 */
@Data
public class IntelligenceRes {

    private String entityId;

    private String entityType;

    private String displayName;

    private String primaryKey;

    private String density;

    private List<EntityLabelRes> labels;

    private List<String> sourceSystems;

    @Data
    public static class EntityLabelRes {
        private String labelType;
        private String labelCode;
        private String labelName;
        private String source;
    }
}
