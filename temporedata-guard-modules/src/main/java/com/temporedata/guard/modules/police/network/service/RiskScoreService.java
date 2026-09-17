package com.temporedata.guard.modules.police.network.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;
import com.temporedata.guard.modules.police.network.entity.RiskScoreEntity;
import com.temporedata.guard.modules.police.network.repository.RiskScoreRepository;
import com.temporedata.guard.modules.police.track.service.TrackService;
import com.temporedata.guard.modules.police.watch.service.WatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 重点人员管控积分（P2）：轨迹活跃/通联度/聚集/资金流入/布控命中 多维加权 → 红黄橙分级。
 * 高分人员可联动 watch/alarm 预警建议。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoreService {

    private final RiskScoreRepository riskRepository;
    private final EntityMasterRepository masterRepository;
    private final TrackService trackService;
    private final WatchService watchService;
    private final GraphNetworkService networkService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 指标权重 */
    private static final Map<String, Integer> WEIGHTS = Map.of(
            "traffic", 25, "contact", 25, "gather", 20, "fund", 20, "watch", 30);

    /** 重评全部人员(定时, 每 30 分钟) */
    @Scheduled(cron = "0 */30 * * * ?")
    public void scheduledReevaluate() {
        for (EntityMasterEntity e : masterRepository.findAll()) {
            if ("PERSON".equals(e.getEntityType())) {
                try {
                    evaluate(e.getId(), e.getTenantId());
                } catch (Exception ex) {
                    log.warn("Risk evaluate failed for {}: {}", e.getId(), ex.getMessage());
                }
            }
        }
    }

    /** 评估单个人员: 采集指标 → 加权 → 分级 → 存档 */
    @Transactional
    public RiskScoreEntity evaluate(String entityId, String tenantId) {
        EntityMasterEntity e = masterRepository.findById(entityId)
                .filter(x -> x.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("实体不存在"));

        Map<String, Integer> factors = new LinkedHashMap<>();
        LocalDateTime from = LocalDateTime.now().minusDays(30);

        // 1. 轨迹活跃度(近30天点数, ≥10 记满分)
        int points = trackService.query(entityId, from, LocalDateTime.now(), tenantId).size();
        factors.put("traffic", Math.min(100, points * 10));

        // 2. 通联度(度数)
        long contactDegree = networkService.edgesOf(entityId, tenantId).stream()
                .filter(x -> "CONTACT".equals(x.getRelationType())).count();
        factors.put("contact", Math.min(100, (int) contactDegree * 25));

        // 3. 聚集(共址边)
        long gather = networkService.edgesOf(entityId, tenantId).stream()
                .filter(x -> "CO_LOCATE".equals(x.getRelationType())).count();
        factors.put("gather", Math.min(100, (int) gather * 33));

        // 4. 资金流入(流入金额 > 1万 记分)
        double fundIn = networkService.edgesOf(entityId, tenantId).stream()
                .filter(x -> "FUND".equals(x.getRelationType()))
                .mapToDouble(x -> x.getWeight() == null ? 0 : x.getWeight()).sum();
        factors.put("fund", Math.min(100, (int) (fundIn / 10000 * 20)));

        // 5. 布控命中(在布控名单 → 高风险)
        boolean watched = !watchService.matchLists(entityId, tenantId).isEmpty();
        factors.put("watch", watched ? 100 : 0);

        int score = factors.entrySet().stream()
                .mapToInt(f -> Math.min(100, f.getValue()) * WEIGHTS.getOrDefault(f.getKey(), 10)).sum() / 100;
        String level = level(score);

        RiskScoreEntity risk = riskRepository.findFirstByEntityIdOrderByUpdateTimeDesc(entityId).orElseGet(() -> {
            RiskScoreEntity r = new RiskScoreEntity();
            r.setId(UUID.randomUUID().toString().replace("-", ""));
            r.setEntityId(entityId);
            r.setTenantId(tenantId);
            r.setCreateTime(LocalDateTime.now());
            return r;
        });
        risk.setEntityName(e.getDisplayName());
        risk.setScore(score);
        risk.setRiskLevel(level);
        try {
            risk.setFactors(objectMapper.writeValueAsString(factors));
        } catch (Exception ex) {
            risk.setFactors(String.valueOf(factors));
        }
        risk.setUpdateTime(LocalDateTime.now());
        RiskScoreEntity saved = riskRepository.save(risk);
        if ("RED".equals(level)) {
            log.warn("[RISK] {} 高风险({}分): {}", e.getDisplayName(), score, factors);
        }
        return saved;
    }

    public List<RiskScoreEntity> list(String tenantId, String level) {
        return level != null && !level.isBlank()
                ? riskRepository.findByTenantIdAndRiskLevelOrderByScoreDesc(tenantId, level)
                : riskRepository.findByTenantIdOrderByScoreDesc(tenantId);
    }

    public RiskScoreEntity detail(String entityId, String tenantId) {
        return riskRepository.findFirstByEntityIdOrderByUpdateTimeDesc(entityId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("尚无风险评分，请先评估"));
    }

    private String level(int score) {
        if (score >= 70) return "RED";
        if (score >= 45) return "YELLOW";
        if (score >= 25) return "ORANGE";
        return "LOW";
    }
}
