package com.temporedata.guard.modules.police.network.service;

import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.modules.police.network.entity.RiskScoreEntity;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 涉诈专项研判（P3）：组合 风险积分 + 资金流 + 涉诈标签 + 关联团伙 → 研判卡。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private final IntelligenceService intelligenceService;
    private final RiskScoreService riskScoreService;
    private final FundFlowService fundFlowService;
    private final ContactAnalysisService contactAnalysisService;
    private final EntityLabelRepository labelRepository;

    /** 涉诈研判: 关键词定位对象 → 聚合多源 → 研判卡 */
    public Map<String, Object> analyze(String keyword, LoginUser user) {
        var hits = intelligenceService.search(keyword, null, user);
        if (hits.isEmpty()) return Map.of("error", "未找到实体");
        IntelligenceRes target = hits.get(0);
        String entityId = target.getEntityId();

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("entity", target.getDisplayName());
        card.put("entityId", entityId);

        // 1. 涉诈标签
        List<String> labels = labelRepository.findByEntityId(entityId).stream()
                .map(EntityLabelEntity::getLabelName).filter(n -> n != null).collect(Collectors.toList());
        card.put("labels", labels);

        // 2. 风险等级(红黄橙)
        try {
            RiskScoreEntity risk = riskScoreService.detail(entityId, user.getTenantId());
            card.put("riskLevel", risk.getRiskLevel());
            card.put("riskScore", risk.getScore());
        } catch (Exception e) {
            card.put("riskLevel", "UNKNOWN");
            card.put("riskScore", null);
        }

        // 3. 资金链(该实体为端点的 FUND 边)
        card.put("fundEdges", fundFlowService.trace(entityId, 2, null, user));

        // 4. 关联团伙(通联社群成员)
        card.put("gang", contactAnalysisService.analyzeByKeyword(target.getDisplayName(), user).getOrDefault("k3Neighbors", List.of()));

        // 5. 研判结论(规则)
        String conclusion = buildConclusion(card);
        card.put("conclusion", conclusion);
        return card;
    }

    private String buildConclusion(Map<String, Object> card) {
        String level = String.valueOf(card.getOrDefault("riskLevel", "UNKNOWN"));
        boolean hasFraudLabel = ((List<?>) card.getOrDefault("labels", List.of())).stream()
                .anyMatch(l -> String.valueOf(l).contains("诈") || String.valueOf(l).contains("骗"));
        int fundEdges = ((List<?>) card.getOrDefault("fundEdges", List.of())).size();
        int gang = ((List<?>) card.getOrDefault("gang", List.of())).size();

        if ("RED".equals(level) && hasFraudLabel) {
            return "高危涉诈对象：风险等级RED且具涉诈标签，建议立即布控并冻结关联账户";
        }
        if ("RED".equals(level) || "YELLOW".equals(level) && (fundEdges > 0 || gang > 0)) {
            return "重点关注：风险等级" + level + "，存在资金关联与通联团伙，建议持续监控并研判资金去向";
        }
        return "风险等级" + level + "，暂未发现明显涉诈特征，建议保持常规关注";
    }
}
