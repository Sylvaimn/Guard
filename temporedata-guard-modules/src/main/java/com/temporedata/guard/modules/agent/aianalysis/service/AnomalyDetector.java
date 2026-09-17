/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AnomalyClueEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AnomalyClueRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;


import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;
import com.temporedata.guard.modules.police.watch.service.WatchService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyDetector {
    private static final Logger log = LoggerFactory.getLogger(AnomalyDetector.class);
    private final AnomalyClueRepository clueRepository;
    private final QualityRuleRepository qualityRuleRepository;
    private final WatchService watchService;

    @Scheduled(cron="0 */30 * * * ?")
    public void scheduledDetect() {
        try {
            List<String> tenants = this.qualityRuleRepository.findAll().stream().map(r -> r.getTenantId()).distinct().collect(Collectors.toList());
            for (String tenantId : tenants) {
                this.detect(tenantId);
            }
        }
        catch (Exception e) {
            log.warn("Scheduled anomaly detect failed: {}", (Object)e.getMessage());
        }
    }

    @Transactional
    public void detect(String tenantId) {
        double rate;
        long total = this.qualityRuleRepository.count();
        long failed = this.qualityRuleRepository.countByLastStatus("FAIL");
        if (total >= 3L && failed > 0L && (rate = (double)failed * 100.0 / (double)total) >= 50.0) {
            this.createClue(tenantId, "QUALITY_FAILURE", "\u8d28\u91cf\u89c4\u5219\u5931\u8d25\u7387", 0.0, rate, rate, "\u5b58\u5728 " + failed + "/" + total + " \u6761\u8d28\u91cf\u89c4\u5219\u5931\u8d25\uff0c\u5efa\u8bae\u6392\u67e5\u6570\u636e\u6e90\u4e0e\u89c4\u5219\u914d\u7f6e");
        }
        Map<String, Object> stats = this.watchService.stats(tenantId);
        long open = ((Number)stats.getOrDefault("pending", 0L)).longValue() + ((Number)stats.getOrDefault("processing", 0L)).longValue();
        long closed = ((Number)stats.getOrDefault("closed", 0L)).longValue();
        if (open >= 5L && open > closed) {
            double ratio = (double)open * 100.0 / (double)Math.max(closed, 1L);
            this.createClue(tenantId, "WATCH_BACKLOG", "\u5e03\u63a7\u9884\u8b66\u5904\u7f6e\u79ef\u538b", (double)closed * 1.0, (double)open * 1.0, ratio, "\u5f85\u5904\u7f6e\u9884\u8b66 " + open + " \u6761\uff08\u5df2\u95ed\u73af " + closed + "\uff09\uff0c\u5efa\u8bae\u589e\u6d3e\u8b66\u529b\u7b7e\u6536\u5904\u7f6e");
        }
    }

    @Transactional
    public AnomalyClueEntity createClue(String tenantId, String metricType, String target, double baseline, double current, double deviation, String action) {
        if (this.clueRepository.findFirstByTenantIdAndMetricTypeAndTargetAndStatus(tenantId, metricType, target, "OPEN").isPresent()) {
            return null;
        }
        AnomalyClueEntity clue = new AnomalyClueEntity();
        clue.setId(UUID.randomUUID().toString().replace("-", ""));
        clue.setMetricType(metricType);
        clue.setTarget(target);
        clue.setBaseline(baseline);
        clue.setCurrent(current);
        clue.setDeviation(deviation);
        clue.setStatus("OPEN");
        clue.setSuggestedAction(action);
        clue.setTenantId(tenantId);
        clue.setCreateTime(LocalDateTime.now());
        AnomalyClueEntity saved = (AnomalyClueEntity)this.clueRepository.save(clue);
        log.info("[ANOMALY] clue generated: {} {} baseline={} current={}", new Object[]{metricType, target, baseline, current});
        return saved;
    }

    public List<AnomalyClueEntity> list(String tenantId, String status) {
        return status != null && !status.isBlank() ? this.clueRepository.findByTenantIdAndStatusOrderByCreateTimeDesc(tenantId, status) : this.clueRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    @Transactional
    public AnomalyClueEntity confirm(String id, String tenantId) {
        AnomalyClueEntity clue = this.get(id, tenantId);
        clue.setStatus("CONFIRMED");
        clue.setUpdateTime(LocalDateTime.now());
        return (AnomalyClueEntity)this.clueRepository.save(clue);
    }

    @Transactional
    public AnomalyClueEntity dismiss(String id, String tenantId) {
        AnomalyClueEntity clue = this.get(id, tenantId);
        clue.setStatus("DISMISSED");
        clue.setUpdateTime(LocalDateTime.now());
        return (AnomalyClueEntity)this.clueRepository.save(clue);
    }

    private AnomalyClueEntity get(String id, String tenantId) {
        AnomalyClueEntity clue = (AnomalyClueEntity)this.clueRepository.findById(id).orElseThrow(() -> new BusinessException("\u7ebf\u7d22\u4e0d\u5b58\u5728"));
        if (!clue.getTenantId().equals(tenantId)) {
            throw new BusinessException("\u65e0\u6743\u8bbf\u95ee\u8be5\u7ebf\u7d22");
        }
        return clue;
    }

    public AnomalyDetector(AnomalyClueRepository clueRepository, QualityRuleRepository qualityRuleRepository, WatchService watchService) {
        this.clueRepository = clueRepository;
        this.qualityRuleRepository = qualityRuleRepository;
        this.watchService = watchService;
    }
}

