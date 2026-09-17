/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.loginlog.LoginLogRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.loginlog.service;
import com.temporedata.guard.modules.security.loginlog.entity.LoginLogEntity;
import com.temporedata.guard.modules.security.loginlog.repository.LoginLogRepository;

import com.temporedata.guard.api.loginlog.LoginLogRes;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginLogService {
    private static final Logger log = LoggerFactory.getLogger(LoginLogService.class);
    private final LoginLogRepository loginLogRepository;

    public Page<LoginLogRes> listAll(int page, int size, String tenantId) {
        return this.loginLogRepository.findByTenantIdOrderByCreateTimeDesc(tenantId, (Pageable)PageRequest.of((int)page, (int)size)).map(this::toRes);
    }

    @Transactional
    public void logSuccess(String userId, String username, String loginType, String loginIp, String userAgent, String tenantId) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setLoginType(loginType);
        entity.setLoginIp(loginIp);
        entity.setUserAgent(userAgent);
        entity.setStatus("SUCCESS");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        this.loginLogRepository.save(entity);
    }

    @Transactional
    public void logFailure(String username, String loginType, String loginIp, String userAgent, String failReason, String tenantId) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUsername(username);
        entity.setLoginType(loginType);
        entity.setLoginIp(loginIp);
        entity.setUserAgent(userAgent);
        entity.setStatus("FAILED");
        entity.setFailReason(failReason);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        this.loginLogRepository.save(entity);
    }

    public List<LoginLogRes> getByUserId(String userId) {
        return this.loginLogRepository.findByUserIdOrderByCreateTimeDesc(userId).stream().map(this::toRes).collect(Collectors.toList());
    }

    private LoginLogRes toRes(LoginLogEntity e) {
        LoginLogRes res = new LoginLogRes();
        res.setId(e.getId());
        res.setUserId(e.getUserId());
        res.setUsername(e.getUsername());
        res.setLoginType(e.getLoginType());
        res.setLoginIp(e.getLoginIp());
        res.setUserAgent(e.getUserAgent());
        res.setStatus(e.getStatus());
        res.setFailReason(e.getFailReason());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }
}

