package com.temporedata.guard.api.agent;

import lombok.Data;

import java.util.List;

/**
 * 实体详情卡片（P1，原型"查看详情"结构化展示）。
 */
@Data
public class EntityCardRes {

    private String entityId;

    private String entityType;

    private String displayName;

    /** 主标识(按用户密级脱敏) */
    private String primaryKey;

    /** 关联聚合统计 */
    private Relations relations;

    /** 卡片动作 */
    private List<String> actions;

    @Data
    public static class Relations {
        private int persons;
        private int vehicles;
        private int cases;
        private int items;
        private int orgs;
        private int places;
        private int total;
    }
}
