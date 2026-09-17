/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.intelligence.IntelligenceRes
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiRuleEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AiRuleRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.intelligence.IntelligenceRes;


import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.security.LoginUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Nl2RuleService {
    private static final Logger log = LoggerFactory.getLogger(Nl2RuleService.class);
    private final AiRuleRepository ruleRepository;
    private final IntelligenceService intelligenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SCENE_WORDS = "\u5361\u53e3|\u9152\u5e97|\u5c0f\u533a|\u5546\u573a|\u53e3\u5cb8|\u9ad8\u94c1\u7ad9|\u673a\u573a|\u52a0\u6cb9\u7ad9|\u7f51\u5427|\u5bbe\u9986";
    private static final Pattern COUNT_PATTERN = Pattern.compile("(?:\u8d85\u8fc7|\u5927\u4e8e|\u591a\u4e8e|\u4e0d\u4f4e\u4e8e)?\\s*(\\d+)\\s*\u6b21");
    private static final Pattern AREA_PATTERN = Pattern.compile("\u51fa\u73b0\u5728\\s*([^\uff0c\u3002\uff1b,;]{1,10}?)(?:\u4e14|\u5e76|\u540c\u65f6|\u548c|\u65f6|\u5f53|\uff0c|\u3002|\uff1b|,|;)");
    private static final Pattern LEAD_PATTERN = Pattern.compile("^(?:\u5f53|\u5982\u679c|\u53ea\u8981|\u4e00\u65e6)\\s*");

    public Map<String, Object> parse(String nluText, LoginUser user) {
        if (nluText == null || nluText.isBlank()) {
            throw new BusinessException("\u89c4\u5219\u6587\u672c\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String text = nluText.trim();
        String entityKeyword = LEAD_PATTERN.matcher(text).replaceFirst("").trim();
        int cut = entityKeyword.indexOf("\u51fa\u73b0\u5728");
        if (cut < 0) {
            cut = entityKeyword.indexOf("\u8fdb\u5165");
        }
        if (cut < 0) {
            cut = entityKeyword.indexOf("\u5230\u8fbe");
        }
        if (cut > 0) {
            entityKeyword = entityKeyword.substring(0, cut).trim();
        } else {
            Matcher em = Pattern.compile("^[^\uff0c\u3002\uff1b,;\\s]{2,8}").matcher(entityKeyword);
            if (em.find()) {
                entityKeyword = em.group();
            }
        }
        List<IntelligenceRes> hits = this.intelligenceService.search(entityKeyword, null, user);
        if (hits.isEmpty()) {
            throw new BusinessException("\u65e0\u6cd5\u8bc6\u522b\u89c4\u5219\u5bf9\u8c61(\u5b9e\u4f53): " + entityKeyword);
        }
        int minCount = 1;
        Matcher cm = COUNT_PATTERN.matcher(text);
        if (cm.find()) {
            minCount = Integer.parseInt(cm.group(1));
        }
        String area = "";
        Matcher am = AREA_PATTERN.matcher(text);
        if (am.find()) {
            area = am.group(1).trim();
        }
        String scene = "";
        Matcher sm = Pattern.compile("(\u5361\u53e3|\u9152\u5e97|\u5c0f\u533a|\u5546\u573a|\u53e3\u5cb8|\u9ad8\u94c1\u7ad9|\u673a\u573a|\u52a0\u6cb9\u7ad9|\u7f51\u5427|\u5bbe\u9986)").matcher(text);
        if (sm.find()) {
            scene = sm.group(1);
        }
        LinkedHashMap<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("entityId", hits.get(0).getEntityId());
        rule.put("entityName", hits.get(0).getDisplayName());
        rule.put("area", area);
        rule.put("minCount", minCount);
        rule.put("scene", scene);
        return rule;
    }

    @Transactional
    public AiRuleEntity create(String nluText, Map<String, Object> ruleJson, LoginUser user) {
        AiRuleEntity r = new AiRuleEntity();
        r.setId(UUID.randomUUID().toString().replace("-", ""));
        r.setNluText(nluText);
        try {
            r.setRuleJson(this.objectMapper.writeValueAsString(ruleJson));
        }
        catch (Exception e) {
            throw new BusinessException("\u89c4\u5219JSON\u5e8f\u5217\u5316\u5931\u8d25");
        }
        r.setTenantId(user.getTenantId());
        r.setCreatorId(user.getUserId());
        r.setCreateTime(LocalDateTime.now());
        return (AiRuleEntity)this.ruleRepository.save(r);
    }

    public List<AiRuleEntity> list(String tenantId, String status) {
        return status != null && !status.isBlank() ? this.ruleRepository.findByTenantIdAndStatus(tenantId, status) : this.ruleRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    @Transactional
    public AiRuleEntity setStatus(String id, String tenantId, String status) {
        AiRuleEntity r = this.get(id, tenantId);
        r.setStatus(status);
        r.setUpdateTime(LocalDateTime.now());
        return (AiRuleEntity)this.ruleRepository.save(r);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        this.ruleRepository.delete(this.get(id, tenantId));
    }

    @Transactional
    public Map<String, Object> evaluate(Map<String, String> event, LoginUser user) {
        String entityId = event.get("entityId");
        String scene = event.get("scene") == null ? "" : event.get("scene");
        String address = event.get("address") == null ? "" : event.get("address");
        List<AiRuleEntity> enabled = this.ruleRepository.findByTenantIdAndStatus(user.getTenantId(), "ENABLED");
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        int hits = 0;
        for (AiRuleEntity r : enabled) {
            try {
                String ruleArea;
                Map rule = (Map)this.objectMapper.readValue(r.getRuleJson(), Map.class);
                if (!entityId.equals(rule.get("entityId"))) continue;
                String ruleScene = rule.get("scene") == null ? "" : String.valueOf(rule.get("scene"));
                String string = ruleArea = rule.get("area") == null ? "" : String.valueOf(rule.get("area"));
                if (!ruleScene.isBlank() && !ruleScene.equals(scene) || !ruleArea.isBlank() && !address.contains(ruleArea) && !scene.contains(ruleArea)) continue;
                r.setHitCount((r.getHitCount() == null ? 0 : r.getHitCount()) + 1);
                this.ruleRepository.save(r);
                ++hits;
                log.info("[RULE-HIT] rule {} matched event entity={} scene={}", new Object[]{r.getId(), entityId, scene});
            }
            catch (Exception e) {
                log.warn("Rule eval failed: {}", (Object)e.getMessage());
            }
        }
        out.put("hits", hits);
        return out;
    }

    private AiRuleEntity get(String id, String tenantId) {
        AiRuleEntity r = (AiRuleEntity)this.ruleRepository.findById(id).orElseThrow(() -> new BusinessException("\u89c4\u5219\u4e0d\u5b58\u5728"));
        if (!r.getTenantId().equals(tenantId)) {
            throw new BusinessException("\u65e0\u6743\u8bbf\u95ee\u8be5\u89c4\u5219");
        }
        return r;
    }

    public Nl2RuleService(AiRuleRepository ruleRepository, IntelligenceService intelligenceService) {
        this.ruleRepository = ruleRepository;
        this.intelligenceService = intelligenceService;
    }
}

