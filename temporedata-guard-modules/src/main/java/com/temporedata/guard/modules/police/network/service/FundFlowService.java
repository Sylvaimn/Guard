package com.temporedata.guard.modules.police.network.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
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
 * 资金流分析专项（P1）：FUND 边录入、链路追踪(穿透)、流向聚合、关键账户。
 * 组合关系网络底座。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundFlowService {

    private final GraphNetworkService networkService;
    private final GraphEdgeRepository edgeRepository;
    private final IntelligenceService intelligenceService;

    /** 录入资金边(卡/账户间转账) */
    @Transactional
    public GraphEdgeEntity addFundEdge(String fromAccount, String toAccount, Double amount, String evidence, LoginUser user) {
        if (amount == null || amount <= 0) throw new BusinessException("转账金额必须大于0");
        return networkService.addEdge(fromAccount, toAccount, "FUND", amount, evidence, user.getTenantId());
    }

    /** 资金穿透: 从账户出发多层追踪资金流向 */
    public List<Map<String, Object>> trace(String accountId, int maxDepth, Double minAmount, LoginUser user) {
        return networkService.trace(accountId, "FUND", maxDepth == 0 ? 3 : maxDepth, minAmount, user.getTenantId());
    }

    /** 资金流向聚合: 按目标账户汇总流入金额(Top) */
    public List<Map<String, Object>> inflowAggregate(String tenantId, int topN) {
        List<GraphEdgeEntity> edges = edgeRepository.findByRelationTypeAndTenantIdOrderByWeightDesc("FUND", tenantId);
        return edges.stream()
                .collect(Collectors.groupingBy(GraphEdgeEntity::getTargetEntityId,
                        Collectors.summingDouble(e -> e.getWeight() == null ? 0 : e.getWeight())))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN == 0 ? 10 : topN)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("account", e.getKey());
                    m.put("totalIn", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** Agent 工具: 按关键词定位账户后穿透 */
    public Map<String, Object> analyzeByKeyword(String keyword, int maxDepth, LoginUser user) {
        var hits = intelligenceService.search(keyword, null, user);
        if (hits.isEmpty()) return Map.of("error", "未找到实体");
        String entityId = hits.get(0).getEntityId();
        return Map.of("entity", hits.get(0).getDisplayName(), "chain", trace(entityId, maxDepth, null, user));
    }
}
