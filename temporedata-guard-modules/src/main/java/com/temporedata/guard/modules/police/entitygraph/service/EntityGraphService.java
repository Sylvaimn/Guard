package com.temporedata.guard.modules.police.entitygraph.service;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMappingEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMappingRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.entitygraph.EntityGraphRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 实体关联图（警用）：
 * 服务端裁剪子图——从根实体按 N 度扩展，聚合关联边，返回 ≤ 裁剪上限的节点。
 * 关联推断规则（基于归一化映射与标签）：
 *   - 共用同一来源记录（SHARED_SOURCE）
 *   - 案件标签相同（SAME_CASE）
 *   - 行为标签互现（CONTACT）
 * V2 优化：邻居发现与标签/映射加载全部批量 SQL，消除全表扫描与 N+1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityGraphService {

    private final EntityMasterRepository masterRepository;
    private final EntityMappingRepository mappingRepository;
    private final EntityLabelRepository labelRepository;

    private static final int MAX_NODES = 500;
    private static final int MAX_DEGREE = 4;

    public EntityGraphRes getGraph(String rootEntityId, Integer degree, String edgeType, String tenantId) {
        EntityMasterEntity root = masterRepository.findById(rootEntityId)
                .orElseThrow(() -> new BusinessException("实体不存在"));
        if (!root.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权访问该实体");
        }
        int maxDegree = degree == null ? 1 : Math.min(degree, MAX_DEGREE);

        Map<String, EntityMasterEntity> nodesById = new LinkedHashMap<>();
        List<EntityGraphRes.EntityEdge> edges = new ArrayList<>();
        nodesById.put(root.getId(), root);

        // BFS 扩展
        Deque<EntityMasterEntity> queue = new ArrayDeque<>();
        queue.add(root);
        Map<String, Integer> depthById = new HashMap<>();
        depthById.put(root.getId(), 0);
        Set<String> visitedEdges = new HashSet<>();
        // 标签/映射缓存（按层批量加载，避免 N+1）
        Map<String, List<EntityLabelEntity>> labelCache = new HashMap<>();
        Map<String, List<EntityMappingEntity>> mappingCache = new HashMap<>();
        loadIntoCache(labelCache, mappingCache, List.of(root.getId()));

        while (!queue.isEmpty() && nodesById.size() < MAX_NODES) {
            EntityMasterEntity current = queue.poll();
            int curDepth = depthById.get(current.getId());
            if (curDepth >= maxDegree) break;

            List<EntityMasterEntity> neighbors = findNeighbors(current, tenantId);
            List<String> newIds = neighbors.stream().map(EntityMasterEntity::getId)
                    .filter(id -> !nodesById.containsKey(id)).collect(Collectors.toList());
            if (!newIds.isEmpty()) {
                loadIntoCache(labelCache, mappingCache, newIds);
            }
            for (EntityMasterEntity nb : neighbors) {
                if (nb.getId().equals(current.getId())) continue;
                String edgeKey = current.getId() + "|" + nb.getId();
                if (visitedEdges.contains(edgeKey)) continue;
                visitedEdges.add(edgeKey);
                visitedEdges.add(nb.getId() + "|" + current.getId());

                String rel = inferRelation(current, nb, labelCache, mappingCache);
                if (edgeType != null && !edgeType.isBlank() && !edgeType.equals(rel)) continue;

                boolean newNode = !nodesById.containsKey(nb.getId());
                nodesById.putIfAbsent(nb.getId(), nb);
                edges.add(buildEdge(current.getId(), nb.getId(), rel));

                if (newNode) {
                    depthById.put(nb.getId(), curDepth + 1);
                    if (curDepth + 1 < maxDegree) queue.add(nb);
                }
            }
        }

        EntityGraphRes res = new EntityGraphRes();
        res.setRoot(toNode(root, labelCache.getOrDefault(root.getId(), List.of())));
        res.setNodes(nodesById.values().stream()
                .map(e -> toNode(e, labelCache.getOrDefault(e.getId(), List.of())))
                .collect(Collectors.toList()));
        res.setEdges(edges);
        res.setTrimmed(nodesById.size() >= MAX_NODES || edges.size() > 2000);
        return res;
    }

    /** 邻居发现：共用来源记录 + 相同案件标签（全部批量 SQL） */
    private List<EntityMasterEntity> findNeighbors(EntityMasterEntity entity, String tenantId) {
        Set<String> neighborIds = new LinkedHashSet<>();
        // 1. 共用来源记录（同源同记录ID → 强关联）
        List<String> sourceRecordIds = mappingRepository.findByEntityId(entity.getId()).stream()
                .map(EntityMappingEntity::getSourceRecordId).collect(Collectors.toList());
        if (!sourceRecordIds.isEmpty()) {
            mappingRepository.findBySourceRecordIdIn(sourceRecordIds).stream()
                    .map(EntityMappingEntity::getEntityId)
                    .filter(id -> !id.equals(entity.getId()))
                    .forEach(neighborIds::add);
        }
        // 2. 相同案件标签
        List<String> caseLabelCodes = labelRepository.findByEntityId(entity.getId()).stream()
                .filter(l -> "CASE".equals(l.getLabelType()))
                .map(EntityLabelEntity::getLabelCode)
                .collect(Collectors.toList());
        if (!caseLabelCodes.isEmpty()) {
            labelRepository.findByLabelTypeAndLabelCodeInAndTenantId("CASE", caseLabelCodes, tenantId).stream()
                    .map(EntityLabelEntity::getEntityId)
                    .filter(id -> !id.equals(entity.getId()))
                    .forEach(neighborIds::add);
        }
        return masterRepository.findByIdIn(neighborIds);
    }

    /** 关系推断（使用缓存数据，不再逐实体查库） */
    private String inferRelation(EntityMasterEntity a, EntityMasterEntity b,
                                 Map<String, List<EntityLabelEntity>> labelCache,
                                 Map<String, List<EntityMappingEntity>> mappingCache) {
        Set<String> aCase = labelCache.getOrDefault(a.getId(), List.of()).stream()
                .filter(l -> "CASE".equals(l.getLabelType())).map(EntityLabelEntity::getLabelCode).collect(Collectors.toSet());
        Set<String> bCase = labelCache.getOrDefault(b.getId(), List.of()).stream()
                .filter(l -> "CASE".equals(l.getLabelType())).map(EntityLabelEntity::getLabelCode).collect(Collectors.toSet());
        if (aCase.stream().anyMatch(bCase::contains)) return "SAME_CASE";

        Set<String> aSrc = mappingCache.getOrDefault(a.getId(), List.of()).stream()
                .map(EntityMappingEntity::getSourceRecordId).collect(Collectors.toSet());
        Set<String> bSrc = mappingCache.getOrDefault(b.getId(), List.of()).stream()
                .map(EntityMappingEntity::getSourceRecordId).collect(Collectors.toSet());
        if (aSrc.stream().anyMatch(bSrc::contains)) return "SHARED_SOURCE";

        if ("PERSON".equals(a.getEntityType()) && "VEHICLE".equals(b.getEntityType())) return "USES";
        if ("PERSON".equals(b.getEntityType()) && "VEHICLE".equals(a.getEntityType())) return "USES";
        return "CONTACT";
    }

    /** 批量加载标签与映射到缓存 */
    private void loadIntoCache(Map<String, List<EntityLabelEntity>> labelCache,
                               Map<String, List<EntityMappingEntity>> mappingCache,
                               List<String> entityIds) {
        labelRepository.findByEntityIdIn(entityIds).stream()
                .collect(Collectors.groupingBy(EntityLabelEntity::getEntityId))
                .forEach(labelCache::put);
        mappingRepository.findByEntityIdIn(entityIds).stream()
                .collect(Collectors.groupingBy(EntityMappingEntity::getEntityId))
                .forEach(mappingCache::put);
    }

    private EntityGraphRes.EntityNode toNode(EntityMasterEntity e, List<EntityLabelEntity> labels) {
        EntityGraphRes.EntityNode node = new EntityGraphRes.EntityNode();
        node.setId(e.getId());
        node.setEntityType(e.getEntityType());
        node.setName(e.getDisplayName() != null ? e.getDisplayName() : e.getPrimaryKey());
        node.setPrimaryKey(com.temporedata.guard.common.util.SensitiveUtil.maskKey(e.getPrimaryKey()));
        node.setDensity(e.getDensity());
        node.setLabels(labels.stream()
                .map(EntityLabelEntity::getLabelName).filter(Objects::nonNull).collect(Collectors.toList()));
        return node;
    }

    private EntityGraphRes.EntityEdge buildEdge(String source, String target, String relationType) {
        EntityGraphRes.EntityEdge edge = new EntityGraphRes.EntityEdge();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setRelationType(relationType);
        edge.setLabel(relationLabel(relationType));
        edge.setWeight(1);
        return edge;
    }

    private String relationLabel(String t) {
        switch (t) {
            case "SAME_CASE":
                return "同案";
            case "SHARED_SOURCE":
                return "同源记录";
            case "USES":
                return "使用";
            default:
                return "关联";
        }
    }
}
