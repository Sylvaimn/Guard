/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.changeaudit.ChangeAuditRes
 *  javax.persistence.criteria.Expression
 *  javax.persistence.criteria.Predicate
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.util.StringUtils
 */
package com.temporedata.guard.modules.security.changeaudit.service;
import com.temporedata.guard.modules.security.changeaudit.entity.ChangeAuditEntity;
import com.temporedata.guard.modules.security.changeaudit.repository.ChangeAuditRepository;

import com.temporedata.guard.api.changeaudit.ChangeAuditRes;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChangeAuditService {
    private static final Logger log = LoggerFactory.getLogger(ChangeAuditService.class);
    private final ChangeAuditRepository changeAuditRepository;

    @Transactional
    public void record(String entityType, String entityId, String entityName, String action, String operator, String beforeValue, String afterValue, String tenantId) {
        try {
            ChangeAuditEntity entity = new ChangeAuditEntity();
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setEntityType(entityType);
            entity.setEntityId(entityId);
            entity.setEntityName(entityName);
            entity.setAction(action);
            entity.setOperator(operator);
            entity.setBeforeValue(beforeValue);
            entity.setAfterValue(afterValue);
            entity.setTenantId(tenantId);
            entity.setCreateTime(LocalDateTime.now());
            this.changeAuditRepository.save(entity);
        }
        catch (Exception e) {
            log.warn("Failed to record change audit: {} {} {}", new Object[]{entityType, entityId, action, e});
        }
    }

    public Page<ChangeAuditRes> query(String entityType, String entityId, String action, String operator, String startTime, String endTime, String tenantId, int page, int size) {
        Specification<com.temporedata.guard.modules.security.changeaudit.entity.ChangeAuditEntity> spec = (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal((Expression)root.get("tenantId"), (Object)tenantId));
            if (StringUtils.hasText((String)entityType)) {
                predicates.add(cb.equal((Expression)root.get("entityType"), (Object)entityType));
            }
            if (StringUtils.hasText((String)entityId)) {
                predicates.add(cb.equal((Expression)root.get("entityId"), (Object)entityId));
            }
            if (StringUtils.hasText((String)action)) {
                predicates.add(cb.equal((Expression)root.get("action"), (Object)action));
            }
            if (StringUtils.hasText((String)operator)) {
                predicates.add(cb.like((Expression)root.get("operator"), "%" + operator + "%"));
            }
            if (StringUtils.hasText((String)startTime)) {
                predicates.add(cb.greaterThanOrEqualTo((Expression)root.get("createTime"), (Comparable)LocalDateTime.parse(startTime)));
            }
            if (StringUtils.hasText((String)endTime)) {
                predicates.add(cb.lessThanOrEqualTo((Expression)root.get("createTime"), (Comparable)LocalDateTime.parse(endTime)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<com.temporedata.guard.modules.security.changeaudit.entity.ChangeAuditEntity> result = this.changeAuditRepository.findAll(spec, (Pageable)PageRequest.of((int)page, (int)size, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createTime"})));
        return result.map(this::toRes);
    }

    public List<ChangeAuditRes> listByEntity(String entityType, String entityId, String tenantId) {
        return this.changeAuditRepository.findByEntityTypeAndEntityIdOrderByCreateTimeDesc(entityType, entityId).stream().filter(e -> tenantId.equals(e.getTenantId())).map(this::toRes).collect(Collectors.toList());
    }

    private ChangeAuditRes toRes(ChangeAuditEntity e) {
        ChangeAuditRes res = new ChangeAuditRes();
        res.setId(e.getId());
        res.setEntityType(e.getEntityType());
        res.setEntityId(e.getEntityId());
        res.setEntityName(e.getEntityName());
        res.setAction(e.getAction());
        res.setOperator(e.getOperator());
        res.setBeforeValue(e.getBeforeValue());
        res.setAfterValue(e.getAfterValue());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    public ChangeAuditService(ChangeAuditRepository changeAuditRepository) {
        this.changeAuditRepository = changeAuditRepository;
    }
}

