package com.temporedata.guard.api.entitygraph;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 实体关联图（警用一查多/子图裁剪）：
 * 以归一化实体为节点，业务关系为边。
 */
@Data
public class EntityGraphRes {

    /** 根节点实体 */
    private EntityNode root;

    private List<EntityNode> nodes;

    private List<EntityEdge> edges;

    /** 是否被裁剪（degree/edgeTypes 过滤） */
    private boolean trimmed;

    @Data
    public static class EntityNode {
        private String id;
        private String entityType;
        private String name;
        private String primaryKey;
        private String density;
        private int degree;
        private List<String> labels;
    }

    @Data
    public static class EntityEdge {
        private String source;
        private String target;
        /** 边类型：SAME_CASE / CO_TRAVEL / CONTACT / OWNS / USES / SHARED_SOURCE */
        private String relationType;
        private String label;
        private Integer weight;
    }
}
