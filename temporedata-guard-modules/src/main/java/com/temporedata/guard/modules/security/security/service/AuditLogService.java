/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.security.AuditLogReq
 *  com.temporedata.guard.api.security.AuditLogRes
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
 */
package com.temporedata.guard.modules.security.security.service;

import com.temporedata.guard.api.security.AuditLogReq;
import com.temporedata.guard.api.security.AuditLogRes;
import com.temporedata.guard.modules.security.security.entity.AuditLogEntity;
import com.temporedata.guard.modules.security.security.repository.AuditLogRepository;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogRes> query(AuditLogReq req, String tenantId, int page, int size) {
        Specification<com.temporedata.guard.modules.security.security.entity.AuditLogEntity> spec = (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal((Expression)root.get("tenantId"), (Object)tenantId));
            if (req.getOperator() != null && !req.getOperator().isEmpty()) {
                predicates.add(cb.like((Expression)root.get("operator"), "%" + req.getOperator() + "%"));
            }
            if (req.getActionType() != null && !req.getActionType().isEmpty()) {
                predicates.add(cb.equal((Expression)root.get("actionType"), (Object)req.getActionType()));
            }
            if (req.getTargetType() != null && !req.getTargetType().isEmpty()) {
                predicates.add(cb.equal((Expression)root.get("targetType"), (Object)req.getTargetType()));
            }
            if (req.getTargetName() != null && !req.getTargetName().isEmpty()) {
                predicates.add(cb.like((Expression)root.get("targetName"), "%" + req.getTargetName() + "%"));
            }
            if (req.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo((Expression)root.get("createTime"), (Comparable)req.getStartTime()));
            }
            if (req.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo((Expression)root.get("createTime"), (Comparable)req.getEndTime()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of((int)page, (int)size, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createTime"}));
        return this.auditLogRepository.findAll(spec, (Pageable)pageRequest).map(e -> toRes(e));
    }

    @Transactional
    public void record(String operator, String actionType, String actionDetail, String targetType, String targetName, String ipAddress, String result, String errorMsg, String tenantId) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setOperator(operator);
        entity.setActionType(actionType);
        entity.setActionDetail(actionDetail);
        entity.setTargetType(targetType);
        entity.setTargetName(targetName);
        entity.setIpAddress(ipAddress);
        entity.setResult(result);
        entity.setErrorMsg(errorMsg);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        this.auditLogRepository.save(entity);
    }

    @Transactional
    public void deleteAll(String tenantId) {
        List<AuditLogEntity> logs = this.auditLogRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
        this.auditLogRepository.deleteAll(logs);
        log.info("Cleared all audit logs for tenant: {}", (Object)tenantId);
    }

    private AuditLogRes toRes(AuditLogEntity e) {
        AuditLogRes res = new AuditLogRes();
        res.setId(e.getId());
        res.setOperator(e.getOperator());
        res.setActionType(e.getActionType());
        res.setActionDetail(e.getActionDetail());
        res.setTargetType(e.getTargetType());
        res.setTargetName(e.getTargetName());
        res.setIpAddress(e.getIpAddress());
        res.setResult(e.getResult());
        res.setErrorMsg(e.getErrorMsg());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setDurationMs(e.getDurationMs());
        return res;
    }

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
}

