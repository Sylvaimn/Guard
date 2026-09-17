/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.apilog.ApiLogReq
 *  com.temporedata.guard.api.apilog.ApiLogRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.apilog.service;
import com.temporedata.guard.modules.security.apilog.entity.ApiLogEntity;
import com.temporedata.guard.modules.security.apilog.repository.ApiLogRepository;

import com.temporedata.guard.api.apilog.ApiLogReq;
import com.temporedata.guard.api.apilog.ApiLogRes;


import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiLogService {
    private static final Logger log = LoggerFactory.getLogger(ApiLogService.class);
    private final ApiLogRepository apiLogRepository;

    public Page<ApiLogRes> listAll(int page, int size, String tenantId) {
        return this.apiLogRepository.findByTenantIdOrderByCreateTimeDesc(tenantId, (Pageable)PageRequest.of((int)page, (int)size)).map(this::toRes);
    }

    @Transactional
    public void log(ApiLogReq req, String tenantId) {
        ApiLogEntity entity = new ApiLogEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApiName(req.getApiName());
        entity.setApiPath(req.getApiPath());
        entity.setMethod(req.getMethod());
        entity.setRequestIp(req.getRequestIp());
        entity.setRequestParams(req.getRequestParams());
        entity.setResponseCode(req.getResponseCode());
        entity.setResponseBody(req.getResponseBody());
        entity.setCostTime(req.getCostTime());
        entity.setCallerId(req.getCallerId());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        this.apiLogRepository.save(entity);
    }

    @Transactional
    public void clearOldLogs(int daysBefore) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysBefore);
        int deleted = this.apiLogRepository.deleteByCreateTimeBefore(threshold);
        log.info("Cleared {} old API logs (before {})", (Object)deleted, (Object)threshold);
    }

    private ApiLogRes toRes(ApiLogEntity e) {
        ApiLogRes res = new ApiLogRes();
        res.setId(e.getId());
        res.setApiName(e.getApiName());
        res.setApiPath(e.getApiPath());
        res.setMethod(e.getMethod());
        res.setRequestIp(e.getRequestIp());
        res.setRequestParams(e.getRequestParams());
        res.setResponseCode(e.getResponseCode());
        res.setResponseBody(e.getResponseBody());
        res.setCostTime(e.getCostTime());
        res.setCallerId(e.getCallerId());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    public ApiLogService(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }
}

