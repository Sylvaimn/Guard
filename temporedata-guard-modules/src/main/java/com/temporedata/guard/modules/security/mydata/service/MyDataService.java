/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.mydata.MyDataGrantReq
 *  com.temporedata.guard.api.mydata.MyDataRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.mydata.service;
import com.temporedata.guard.modules.security.mydata.entity.MyDataEntity;
import com.temporedata.guard.modules.security.mydata.repository.MyDataRepository;

import com.temporedata.guard.api.mydata.MyDataGrantReq;
import com.temporedata.guard.api.mydata.MyDataRes;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyDataService {
    private static final Logger log = LoggerFactory.getLogger(MyDataService.class);
    private final MyDataRepository myDataRepository;

    public List<MyDataRes> listByUser(String userId) {
        return this.myDataRepository.findByUserId(userId).stream().map(this::toRes).collect(Collectors.toList());
    }

    public List<MyDataRes> listByUserAndType(String userId, String resourceType) {
        return this.myDataRepository.findByUserIdAndResourceType(userId, resourceType).stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public MyDataRes addFavorite(String userId, String resourceType, String resourceId, String resourceName, String tenantId) {
        MyDataEntity entity = new MyDataEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setResourceName(resourceName);
        entity.setAccessType("FAVORITE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((MyDataEntity)this.myDataRepository.save(entity));
    }

    @Transactional
    public void removeFavorite(String userId, String resourceId) {
        this.myDataRepository.findByUserIdAndResourceId(userId, resourceId).ifPresent(entity -> this.myDataRepository.deleteById(entity.getId()));
    }

    @Transactional
    public MyDataRes grantAccess(MyDataGrantReq req, String tenantId, String grantedBy) {
        MyDataEntity entity = new MyDataEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(req.getUserId());
        entity.setResourceType(req.getResourceType());
        entity.setResourceId(req.getResourceId());
        entity.setResourceName(req.getResourceName());
        entity.setAccessType("GRANTED");
        entity.setGrantedBy(grantedBy);
        entity.setExpireTime(req.getExpireTime());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((MyDataEntity)this.myDataRepository.save(entity));
    }

    private MyDataRes toRes(MyDataEntity e) {
        MyDataRes res = new MyDataRes();
        res.setId(e.getId());
        res.setUserId(e.getUserId());
        res.setResourceType(e.getResourceType());
        res.setResourceId(e.getResourceId());
        res.setResourceName(e.getResourceName());
        res.setAccessType(e.getAccessType());
        res.setGrantedBy(e.getGrantedBy());
        res.setExpireTime(e.getExpireTime() != null ? e.getExpireTime().toString() : null);
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    public MyDataService(MyDataRepository myDataRepository) {
        this.myDataRepository = myDataRepository;
    }
}

