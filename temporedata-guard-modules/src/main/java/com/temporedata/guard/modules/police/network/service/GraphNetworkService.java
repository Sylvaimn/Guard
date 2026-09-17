package com.temporedata.guard.modules.police.network.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.police.network.entity.GraphEdgeEntity;
import com.temporedata.guard.modules.police.network.repository.GraphEdgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 关系网络底座（P1）：统一边模型 + 链路追踪/社群发现/中心度/k度关联。
 * 资金流(FUND)、通联(CONTACT)、同案(SAME_CASE)、共址(CO_LOCATE) 共用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphNetworkService {

    private final GraphEdgeRepository edgeRepository;

    /** 录入边(幂等: 同源同目标同类型去重, 更新权重/时间窗) */
    @Transactional
    public GraphEdgeEntity addEdge(String sourceEntityId, String targetEntityId, String relationType,
                                   Double weight, String evidence, String tenantId) {
        if (sourceEntityId == null || targetEntityId == null) throw new BusinessException("边的两端实体必填");
        if (relationType == null) throw new BusinessException("关系类型必填");
        String type = relationType.toUpperCase();
        if (!Set.of("FUND", "CONTACT", "SAME_CASE", "CO_LOCATE").contains(type)) {
            throw new BusinessException("未知关系类型: " + type);
        }
        return edgeRepository.findBySourceEntityIdOrTargetEntityIdOrderByWeightDesc(sourceEntityId, targetEntityId)
                .stream()
                .filter(e -> e.getRelationType().equals(type))
                .filter(e -> (e.getSourceEntityId().equals(sourceEntityId) && e.getTargetEntityId().equals(targetEntityId))
                        || (e.getSourceEntityId().equals(targetEntityId) && e.getTargetEntityId().equals(sourceEntityId)))
                .findFirst()
                .map(e -> {
                    e.setWeight(weight == null ? e.getWeight() : weight);
                    e.setTimeWindow(LocalDateTime.now());
                    e.setEvidence(evidence == null ? e.getEvidence() : evidence);
                    return edgeRepository.save(e);
                })
                .orElseGet(() -> {
                    GraphEdgeEntity e = new GraphEdgeEntity();
                    e.setId(UUID.randomUUID().toString().replace("-", ""));
                    e.setSourceEntityId(sourceEntityId);
                    e.setTargetEntityId(targetEntityId);
                    e.setRelationType(type);
                    e.setWeight(weight);
                    e.setTimeWindow(LocalDateTime.now());
                    e.setEvidence(evidence);
                    e.setTenantId(tenantId);
                    e.setCreateTime(LocalDateTime.now());
                    return edgeRepository.save(e);
                });
    }

    /** 某实体所有边(无向) */
    public List<GraphEdgeEntity> edgesOf(String entityId, String tenantId) {
        return edgeRepository.findBySourceEntityIdOrTargetEntityIdOrderByWeightDesc(entityId, entityId).stream()
                .filter(e -> e.getTenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    /** 某类型全部边 */
    public List<GraphEdgeEntity> edgesByType(String relationType, String tenantId) {
        return edgeRepository.findByRelationTypeAndTenantIdOrderByWeightDesc(relationType.toUpperCase(), tenantId);
    }

    /** 链路追踪(BFS 多层, 如资金穿透/通联扩展) */
    public List<Map<String, Object>> trace(String startEntityId, String relationType, int maxDepth,
                                           Double minWeight, String tenantId) {
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Map<String, Integer> depth = new HashMap<>();
        queue.add(startEntityId);
        visited.add(startEntityId);
        depth.put(startEntityId, 0);
        List<Map<String, Object>> chain = new ArrayList<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int curDepth = depth.get(current);
            if (curDepth >= maxDepth) continue;
            for (GraphEdgeEntity e : edgesOf(current, tenantId)) {
                if (relationType != null && !relationType.isBlank() && !e.getRelationType().equals(relationType.toUpperCase())) {
                    continue;
                }
                if (minWeight != null && e.getWeight() != null && e.getWeight() < minWeight) continue;
                String neighbor = e.getSourceEntityId().equals(current) ? e.getTargetEntityId() : e.getSourceEntityId();
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);
                depth.put(neighbor, curDepth + 1);
                chain.add(Map.of(
                        "from", current, "to", neighbor, "type", e.getRelationType(),
                        "weight", e.getWeight() == null ? 0 : e.getWeight(),
                        "evidence", e.getEvidence() == null ? "" : e.getEvidence(),
                        "depth", curDepth + 1));
                queue.add(neighbor);
            }
        }
        return chain;
    }

    /** 社群发现: 某类型边的连通分量(团伙/资金圈) */
    public List<Map<String, Object>> communities(String relationType, String tenantId) {
        List<GraphEdgeEntity> edges = edgesByType(relationType, tenantId);
        Map<String, String> parent = new HashMap<>();
        for (GraphEdgeEntity e : edges) {
            parent.putIfAbsent(e.getSourceEntityId(), e.getSourceEntityId());
            parent.putIfAbsent(e.getTargetEntityId(), e.getTargetEntityId());
        }
        for (GraphEdgeEntity e : edges) {
            union(parent, e.getSourceEntityId(), e.getTargetEntityId());
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        parent.keySet().forEach(id -> groups.computeIfAbsent(find(parent, id), k -> new ArrayList<>()).add(id));
        return groups.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("size", e.getValue().size());
                    m.put("members", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("size"), (Integer) a.get("size")))
                .collect(Collectors.toList());
    }

    /** 中心度: 某实体在某类型网络中的度(影响力) */
    public Map<String, Object> centrality(String entityId, String relationType, String tenantId) {
        List<GraphEdgeEntity> edges = edgesOf(entityId, tenantId);
        long degree = edges.stream()
                .filter(e -> relationType == null || relationType.isBlank() || e.getRelationType().equals(relationType.toUpperCase()))
                .count();
        double totalWeight = edges.stream()
                .filter(e -> relationType == null || relationType.isBlank() || e.getRelationType().equals(relationType.toUpperCase()))
                .mapToDouble(e -> e.getWeight() == null ? 0 : e.getWeight()).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("entityId", entityId);
        out.put("degree", degree);
        out.put("totalWeight", totalWeight);
        out.put("relationTypes", edges.stream().map(GraphEdgeEntity::getRelationType).distinct().collect(Collectors.toList()));
        return out;
    }

    /** k度关联(k=1..3) */
    public List<Map<String, Object>> kDegreeNeighbors(String entityId, int k, String relationType, String tenantId) {
        return trace(entityId, relationType, k, null, tenantId);
    }

    private void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a), rb = find(parent, b);
        if (!ra.equals(rb)) parent.put(ra, rb);
    }

    private String find(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) {
            parent.put(x, parent.get(parent.get(x)));
            x = parent.get(x);
        }
        return x;
    }
}
