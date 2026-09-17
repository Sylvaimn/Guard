package com.temporedata.guard.modules.police.network.service;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;

import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 相似与聚类引擎（P2）：案件/实体特征相似度 → 串并案建议 → SAME_CASE 边写入网络。
 * 特征: 实体类型/标签集 + 名称文本 n-gram。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final EntityMasterRepository masterRepository;
    private final EntityLabelRepository labelRepository;
    private final IntelligenceService intelligenceService;
    private final GraphNetworkService networkService;

    /** 特征缓存: entityId -> 特征集(内存, 变更时失效由外部 clear) */
    private final Map<String, Map<String, Double>> featureCache = new ConcurrentHashMap<>();

    /** 串并案: 给定关键词 → 候选案件 → 相似度排序 → 建议(自动写 SAME_CASE 边) */
    public Map<String, Object> similarCases(String keyword, LoginUser user, boolean persistEdge) {
        var hits = intelligenceService.search(keyword, null, user);
        if (hits.isEmpty()) return Map.of("error", "未找到实体");
        String baseId = hits.get(0).getEntityId();
        Map<String, Double> base = features(baseId, user);

        List<Map<String, Object>> similar = new ArrayList<>();
        for (EntityMasterEntity e : masterRepository.findAll()) {
            if (!e.getTenantId().equals(user.getTenantId())) continue;
            if (e.getId().equals(baseId)) continue;
            if ("CASE".equals(e.getEntityType())) {
                double sim = cosine(base, features(e.getId(), user));
                if (sim > 0.15) {
                    similar.add(Map.of("entityId", e.getId(), "displayName", e.getDisplayName(),
                            "similarity", Math.round(sim * 100) / 100.0));
                    if (persistEdge && sim > 0.4) {
                        networkService.addEdge(baseId, e.getId(), "SAME_CASE", sim,
                                "串并案建议(相似度" + Math.round(sim * 100) / 100.0 + ")", user.getTenantId());
                    }
                }
            }
        }
        similar.sort((a, b) -> Double.compare((Double) b.get("similarity"), (Double) a.get("similarity")));
        return Map.of("base", hits.get(0).getDisplayName(), "similar", similar.stream().limit(10).collect(Collectors.toList()));
    }

    /** 案件特征: 标签集 + 名称文本 bigram */
    private Map<String, Double> features(String entityId, LoginUser user) {
        return featureCache.computeIfAbsent(entityId, id -> {
            Map<String, Double> f = new ConcurrentHashMap<>();
            List<EntityLabelEntity> labels = labelRepository.findByEntityId(id);
            for (EntityLabelEntity l : labels) {
                if (l.getLabelCode() != null) f.merge("L:" + l.getLabelCode(), 1.0, Double::sum);
            }
            masterRepository.findById(id).ifPresent(e -> {
                String name = e.getDisplayName() == null ? "" : e.getDisplayName();
                String s = name.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", " ");
                for (int i = 0; i < s.length() - 1; i++) {
                    String gram = s.substring(i, i + 2);
                    if (!gram.isBlank()) f.merge("T:" + gram, 1.0, Double::sum);
                }
            });
            return f;
        });
    }

    public void clearCache() {
        featureCache.clear();
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, na = 0, nb = 0;
        for (double v : a.values()) na += v * v;
        for (double v : b.values()) nb += v * v;
        if (na == 0 || nb == 0) return 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double bv = b.get(e.getKey());
            if (bv != null) dot += e.getValue() * bv;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
