/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.sensitive.SensitiveDataReq
 *  com.temporedata.guard.api.sensitive.SensitiveDataRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.sensitive.service;
import com.temporedata.guard.modules.security.sensitive.entity.SensitiveDataEntity;
import com.temporedata.guard.modules.security.sensitive.repository.SensitiveDataRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.sensitive.SensitiveDataReq;
import com.temporedata.guard.api.sensitive.SensitiveDataRes;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveDataService {
    private static final Logger log = LoggerFactory.getLogger(SensitiveDataService.class);
    private final SensitiveDataRepository sensitiveDataRepository;

    public List<SensitiveDataRes> listAll() {
        return this.sensitiveDataRepository.findAll().stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public SensitiveDataRes create(SensitiveDataReq req, String tenantId) {
        SensitiveDataEntity entity = new SensitiveDataEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTableName(req.getTableName());
        entity.setColumnName(req.getColumnName());
        entity.setSensitiveType(req.getSensitiveType());
        entity.setMaskRule(req.getMaskRule());
        entity.setLevel(req.getLevel());
        entity.setDescription(req.getDescription());
        entity.setStatus("ACTIVE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((SensitiveDataEntity)this.sensitiveDataRepository.save(entity));
    }

    @Transactional
    public SensitiveDataRes update(String id, SensitiveDataReq req) {
        SensitiveDataEntity entity = (SensitiveDataEntity)this.sensitiveDataRepository.findById(id).orElseThrow(() -> new BusinessException("Record not found"));
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTableName(req.getTableName());
        entity.setColumnName(req.getColumnName());
        entity.setSensitiveType(req.getSensitiveType());
        entity.setMaskRule(req.getMaskRule());
        entity.setLevel(req.getLevel());
        entity.setDescription(req.getDescription());
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((SensitiveDataEntity)this.sensitiveDataRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        this.sensitiveDataRepository.deleteById(id);
    }

    @Transactional
    public List<SensitiveDataRes> discover(String datasourceId, String tableName, String tenantId) {
        log.info("Auto-discovering sensitive columns for datasource={}, table={}", (Object)datasourceId, (Object)tableName);
        return this.sensitiveDataRepository.findByDatasourceIdAndTableName(datasourceId, tableName).stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public List<SensitiveDataRes> applyMask(String datasourceId, String tableName, String tenantId) {
        log.info("Applying mask rules for datasource={}, table={}", (Object)datasourceId, (Object)tableName);
        return this.sensitiveDataRepository.findByDatasourceIdAndTableName(datasourceId, tableName).stream().map(this::toRes).collect(Collectors.toList());
    }

    private SensitiveDataRes toRes(SensitiveDataEntity e) {
        SensitiveDataRes res = new SensitiveDataRes();
        res.setId(e.getId());
        res.setDatasourceId(e.getDatasourceId());
        res.setTableName(e.getTableName());
        res.setColumnName(e.getColumnName());
        res.setSensitiveType(e.getSensitiveType());
        res.setMaskRule(e.getMaskRule());
        res.setLevel(e.getLevel());
        res.setDescription(e.getDescription());
        res.setStatus(e.getStatus());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }

    public SensitiveDataService(SensitiveDataRepository sensitiveDataRepository) {
        this.sensitiveDataRepository = sensitiveDataRepository;
    }
}

