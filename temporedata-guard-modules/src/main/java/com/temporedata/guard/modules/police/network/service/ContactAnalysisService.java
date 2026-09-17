package com.temporedata.guard.modules.police.network.service;

import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.modules.police.network.entity.GraphEdgeEntity;
import com.temporedata.guard.modules.police.network.repository.GraphEdgeRepository;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通联分析专项（P1）：CONTACT 边录入、频繁通联、社群(团伙)、关键人中心度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactAnalysisService {

    private final GraphNetworkService networkService;
    private final GraphEdgeRepository edgeRepository;
    private final IntelligenceService intelligenceService;

    @Transactional
    public GraphEdgeEntity addContactEdge(String fromEntity, String toEntity, Double frequency, String evidence, LoginUser user) {
        return networkService.addEdge(fromEntity, toEntity, "CONTACT", frequency, evidence, user.getTenantId());
    }

    /** 频繁通联 TopN */
    public List<Map<String, Object>> frequentContacts(String tenantId, int topN) {
        List<GraphEdgeEntity> edges = edgeRepository.findByRelationTypeAndTenantIdOrderByWeightDesc("CONTACT", tenantId);
        return edges.stream().limit(topN == 0 ? 10 : topN).map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("from", e.getSourceEntityId());
            m.put("to", e.getTargetEntityId());
            m.put("frequency", e.getWeight());
            m.put("evidence", e.getEvidence());
            return m;
        }).collect(Collectors.toList());
    }

    /** 通联团伙(社群) */
    public List<Map<String, Object>> gangs(String tenantId) {
        return networkService.communities("CONTACT", tenantId);
    }

    /** 关键人物(按中心度排序) */
    public List<Map<String, Object>> keyPersons(String tenantId, int topN) {
        List<GraphEdgeEntity> edges = edgeRepository.findByRelationTypeAndTenantIdOrderByWeightDesc("CONTACT", tenantId);
        Map<String, Long> degree = edges.stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getSourceEntityId(), e.getTargetEntityId()))
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        return degree.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN == 0 ? 10 : topN)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("entityId", e.getKey());
                    m.put("degree", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** Agent 工具: 通联分析(关键词定位 → 三度关联 + 团伙) */
    public Map<String, Object> analyzeByKeyword(String keyword, LoginUser user) {
        var hits = intelligenceService.search(keyword, null, user);
        if (hits.isEmpty()) return Map.of("error", "未找到实体");
        String entityId = hits.get(0).getEntityId();
        return Map.of(
                "entity", hits.get(0).getDisplayName(),
                "k3Neighbors", networkService.kDegreeNeighbors(entityId, 3, "CONTACT", user.getTenantId()),
                "centrality", networkService.centrality(entityId, "CONTACT", user.getTenantId()));
    }
}
