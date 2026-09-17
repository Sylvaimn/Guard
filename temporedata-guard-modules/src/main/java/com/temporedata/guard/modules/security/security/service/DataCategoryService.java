/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.security.DataCategoryReq
 *  com.temporedata.guard.api.security.DataCategoryRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.security.service;
import com.temporedata.guard.modules.security.security.entity.DataCategoryEntity;
import com.temporedata.guard.modules.security.security.repository.DataCategoryRepository;
import com.temporedata.guard.modules.security.security.entity.DataLevelEntity;
import com.temporedata.guard.modules.security.security.repository.DataLevelRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.security.DataCategoryReq;
import com.temporedata.guard.api.security.DataCategoryRes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataCategoryService {
    private static final Logger log = LoggerFactory.getLogger(DataCategoryService.class);
    private final DataCategoryRepository categoryRepository;
    private final DataLevelRepository levelRepository;

    public List<DataCategoryRes> tree(String tenantId) {
        List<DataCategoryEntity> all = this.categoryRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
        Map<String, DataLevelEntity> levelMap = this.levelRepository.findByTenantIdOrderBySortOrderAsc(tenantId).stream().collect(Collectors.toMap(DataLevelEntity::getId, e -> e));
        HashMap<String, List<DataCategoryEntity>> parentMap = new HashMap<String, List<DataCategoryEntity>>();
        for (DataCategoryEntity e2 : all) {
            String parentId = e2.getParentId() != null ? e2.getParentId() : "root";
            parentMap.computeIfAbsent(parentId, k -> new ArrayList()).add(e2);
        }
        return this.buildTree("root", parentMap, levelMap);
    }

    private List<DataCategoryRes> buildTree(String parentId, Map<String, List<DataCategoryEntity>> parentMap, Map<String, DataLevelEntity> levelMap) {
        List<com.temporedata.guard.modules.security.security.entity.DataCategoryEntity> children = parentMap.getOrDefault(parentId, Collections.emptyList());
        return children.stream().map(e -> {
            DataCategoryRes res = this.toRes((DataCategoryEntity)e, levelMap);
            res.setChildren(this.buildTree(e.getId(), parentMap, levelMap));
            return res;
        }).collect(Collectors.toList());
    }

    @Transactional
    public DataCategoryRes create(DataCategoryReq req, String tenantId) {
        DataCategoryEntity entity = new DataCategoryEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setParentId(req.getParentId());
        entity.setLevelId(req.getLevelId());
        entity.setDescription(req.getDescription());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        Map<String, DataLevelEntity> levelMap = this.levelRepository.findByTenantIdOrderBySortOrderAsc(tenantId).stream().collect(Collectors.toMap(DataLevelEntity::getId, e -> e));
        return this.toRes((DataCategoryEntity)this.categoryRepository.save(entity), levelMap);
    }

    @Transactional
    public DataCategoryRes update(String id, DataCategoryReq req, String tenantId) {
        DataCategoryEntity entity = (DataCategoryEntity)this.categoryRepository.findById(id).orElseThrow(() -> new BusinessException("\u5206\u7c7b\u4e0d\u5b58\u5728"));
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setParentId(req.getParentId());
        entity.setLevelId(req.getLevelId());
        entity.setDescription(req.getDescription());
        entity.setUpdateTime(LocalDateTime.now());
        Map<String, DataLevelEntity> levelMap = this.levelRepository.findByTenantIdOrderBySortOrderAsc(tenantId).stream().collect(Collectors.toMap(DataLevelEntity::getId, e -> e));
        return this.toRes((DataCategoryEntity)this.categoryRepository.save(entity), levelMap);
    }

    @Transactional
    public void delete(String id) {
        this.categoryRepository.deleteById(id);
    }

    private DataCategoryRes toRes(DataCategoryEntity e, Map<String, DataLevelEntity> levelMap) {
        DataCategoryRes res = new DataCategoryRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setCode(e.getCode());
        res.setParentId(e.getParentId());
        res.setDescription(e.getDescription());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        if (e.getLevelId() != null && levelMap.containsKey(e.getLevelId())) {
            DataLevelEntity level = levelMap.get(e.getLevelId());
            res.setLevelName(level.getName());
            res.setLevelCode(level.getCode());
        }
        return res;
    }

    public DataCategoryService(DataCategoryRepository categoryRepository, DataLevelRepository levelRepository) {
        this.categoryRepository = categoryRepository;
        this.levelRepository = levelRepository;
    }
}

