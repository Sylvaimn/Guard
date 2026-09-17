/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiTraceEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AiTraceRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTraceService {
    private static final Logger log = LoggerFactory.getLogger(AiTraceService.class);
    private final AiTraceRepository traceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void record(String userId, String sessionId, String step, String action, Map<String, Object> params, Object result, String tenantId) {
        try {
            AiTraceEntity t = new AiTraceEntity();
            t.setId(UUID.randomUUID().toString().replace("-", ""));
            t.setUserId(userId);
            t.setSessionId(sessionId);
            t.setStep(step);
            t.setAction(action);
            t.setParams(this.truncateJson(params));
            t.setResult(this.truncateJson(result));
            t.setTenantId(tenantId);
            t.setCreateTime(LocalDateTime.now());
            this.traceRepository.save(t);
        }
        catch (Exception e) {
            log.warn("AI trace record failed: {}", (Object)e.getMessage());
        }
    }

    public List<AiTraceEntity> list(String tenantId) {
        return this.traceRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    public List<AiTraceEntity> bySession(String sessionId, String tenantId) {
        List<AiTraceEntity> list = this.traceRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        if (list.stream().anyMatch(t -> !t.getTenantId().equals(tenantId))) {
            throw new BusinessException("\u65e0\u6743\u8bbf\u95ee\u8be5\u8f68\u8ff9");
        }
        return list;
    }

    private String truncateJson(Object o) {
        try {
            String json = this.objectMapper.writeValueAsString(o);
            return json.length() > 1900 ? json.substring(0, 1900) : json;
        }
        catch (Exception e) {
            return String.valueOf(o);
        }
    }

    public AiTraceService(AiTraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }
}

