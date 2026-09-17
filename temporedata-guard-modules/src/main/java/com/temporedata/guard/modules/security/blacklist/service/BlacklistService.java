/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.blacklist.BlacklistReq
 *  com.temporedata.guard.api.blacklist.BlacklistRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.blacklist.service;
import com.temporedata.guard.modules.security.blacklist.entity.BlacklistEntity;
import com.temporedata.guard.modules.security.blacklist.repository.BlacklistRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.blacklist.BlacklistReq;
import com.temporedata.guard.api.blacklist.BlacklistRes;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlacklistService {
    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);
    private final BlacklistRepository blacklistRepository;

    public List<BlacklistRes> listAll(String tenantId) {
        return this.blacklistRepository.findAllByTenantIdOrderByCreateTimeDesc(tenantId).stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public BlacklistRes create(BlacklistReq req, String tenantId) {
        BlacklistEntity entity = new BlacklistEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setIpAddress(req.getIpAddress());
        entity.setListType(req.getListType());
        entity.setReason(req.getReason());
        entity.setTargetType(req.getTargetType());
        entity.setBoundary(req.getBoundary());
        entity.setStatus("ACTIVE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((BlacklistEntity)this.blacklistRepository.save(entity));
    }

    @Transactional
    public BlacklistRes update(String id, BlacklistReq req) {
        BlacklistEntity entity = (BlacklistEntity)this.blacklistRepository.findById(id).orElseThrow(() -> new BusinessException("Record not found"));
        entity.setIpAddress(req.getIpAddress());
        entity.setListType(req.getListType());
        entity.setReason(req.getReason());
        entity.setTargetType(req.getTargetType());
        entity.setBoundary(req.getBoundary());
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((BlacklistEntity)this.blacklistRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        this.blacklistRepository.deleteById(id);
    }

    public boolean isBlocked(String ipAddress) {
        return this.blacklistRepository.findFirstByIpAddressAndListTypeAndStatus(ipAddress, "BLACK", "ACTIVE").isPresent();
    }

    public boolean isWhitelisted(String ipAddress) {
        return this.blacklistRepository.findFirstByIpAddressAndListTypeAndStatus(ipAddress, "WHITE", "ACTIVE").isPresent();
    }

    @Transactional
    public BlacklistRes enable(String id) {
        BlacklistEntity entity = (BlacklistEntity)this.blacklistRepository.findById(id).orElseThrow(() -> new BusinessException("Record not found"));
        entity.setStatus("ACTIVE");
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((BlacklistEntity)this.blacklistRepository.save(entity));
    }

    @Transactional
    public BlacklistRes disable(String id) {
        BlacklistEntity entity = (BlacklistEntity)this.blacklistRepository.findById(id).orElseThrow(() -> new BusinessException("Record not found"));
        entity.setStatus("INACTIVE");
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((BlacklistEntity)this.blacklistRepository.save(entity));
    }

    private BlacklistRes toRes(BlacklistEntity e) {
        BlacklistRes res = new BlacklistRes();
        res.setId(e.getId());
        res.setIpAddress(e.getIpAddress());
        res.setListType(e.getListType());
        res.setReason(e.getReason());
        res.setStatus(e.getStatus());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }

    public BlacklistService(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }
}

