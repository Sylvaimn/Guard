/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.behaviorlog.BehaviorLogRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.behaviorlog.service;
import com.temporedata.guard.modules.security.behaviorlog.entity.BehaviorLogEntity;
import com.temporedata.guard.modules.security.behaviorlog.repository.BehaviorLogRepository;

import com.temporedata.guard.api.behaviorlog.BehaviorLogRes;


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
public class BehaviorLogService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorLogService.class);
    private final BehaviorLogRepository behaviorLogRepository;

    public Page<BehaviorLogRes> listAll(int page, int size, String tenantId) {
        return this.behaviorLogRepository.findByTenantIdOrderByCreateTimeDesc(tenantId, (Pageable)PageRequest.of((int)page, (int)size)).map(this::toRes);
    }

    @Transactional
    public void log(String userId, String username, String action, String targetType, String targetId, String targetName, String detail, String ip, String tenantId) {
        BehaviorLogEntity entity = new BehaviorLogEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setTargetName(targetName);
        entity.setDetail(detail);
        entity.setIp(ip);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        this.behaviorLogRepository.save(entity);
    }

    public List<BehaviorLogRes> getByUserId(String userId) {
        return this.behaviorLogRepository.findByUserIdOrderByCreateTimeDesc(userId).stream().map(this::toRes).collect(Collectors.toList());
    }

    private BehaviorLogRes toRes(BehaviorLogEntity e) {
        BehaviorLogRes res = new BehaviorLogRes();
        res.setId(e.getId());
        res.setUserId(e.getUserId());
        res.setUsername(e.getUsername());
        res.setAction(e.getAction());
        res.setTargetType(e.getTargetType());
        res.setTargetId(e.getTargetId());
        res.setTargetName(e.getTargetName());
        res.setDetail(e.getDetail());
        res.setIp(e.getIp());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    public BehaviorLogService(BehaviorLogRepository behaviorLogRepository) {
        this.behaviorLogRepository = behaviorLogRepository;
    }
}

