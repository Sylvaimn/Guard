/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.security.DataLevelReq
 *  com.temporedata.guard.api.security.DataLevelRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.security.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.security.DataLevelReq;
import com.temporedata.guard.api.security.DataLevelRes;
import com.temporedata.guard.modules.security.security.entity.DataLevelEntity;
import com.temporedata.guard.modules.security.security.repository.DataLevelRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataLevelService {
    private static final Logger log = LoggerFactory.getLogger(DataLevelService.class);
    private final DataLevelRepository levelRepository;

    public List<DataLevelRes> list(String tenantId) {
        return this.levelRepository.findByTenantIdOrderBySortOrderAsc(tenantId).stream().map(this::toRes).collect(Collectors.toList());
    }

    public DataLevelRes detail(String id) {
        return this.toRes((DataLevelEntity)this.levelRepository.findById(id).orElseThrow(() -> new BusinessException("\u5206\u7ea7\u7ea7\u522b\u4e0d\u5b58\u5728")));
    }

    @Transactional
    public DataLevelRes create(DataLevelReq req, String tenantId) {
        if (this.levelRepository.existsByCodeAndTenantId(req.getCode(), tenantId)) {
            throw new BusinessException("\u7ea7\u522b\u4ee3\u7801\u5df2\u5b58\u5728: " + req.getCode());
        }
        DataLevelEntity entity = new DataLevelEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setDescription(req.getDescription());
        entity.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((DataLevelEntity)this.levelRepository.save(entity));
    }

    @Transactional
    public DataLevelRes update(String id, DataLevelReq req, String tenantId) {
        DataLevelEntity entity = (DataLevelEntity)this.levelRepository.findById(id).orElseThrow(() -> new BusinessException("\u5206\u7ea7\u7ea7\u522b\u4e0d\u5b58\u5728"));
        if (!entity.getCode().equals(req.getCode()) && this.levelRepository.existsByCodeAndTenantId(req.getCode(), tenantId)) {
            throw new BusinessException("\u7ea7\u522b\u4ee3\u7801\u5df2\u5b58\u5728: " + req.getCode());
        }
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setDescription(req.getDescription());
        entity.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((DataLevelEntity)this.levelRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        this.levelRepository.deleteById(id);
    }

    private DataLevelRes toRes(DataLevelEntity e) {
        DataLevelRes res = new DataLevelRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setCode(e.getCode());
        res.setDescription(e.getDescription());
        res.setSortOrder(e.getSortOrder());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    public DataLevelService(DataLevelRepository levelRepository) {
        this.levelRepository = levelRepository;
    }
}

