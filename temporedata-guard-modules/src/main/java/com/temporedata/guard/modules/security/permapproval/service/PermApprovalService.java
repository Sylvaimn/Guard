/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.permapproval.PermApprovalReq
 *  com.temporedata.guard.api.permapproval.PermApprovalRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.permapproval.service;
import com.temporedata.guard.modules.security.permapproval.entity.PermApprovalEntity;
import com.temporedata.guard.modules.security.permapproval.repository.PermApprovalRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.permapproval.PermApprovalReq;
import com.temporedata.guard.api.permapproval.PermApprovalRes;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermApprovalService {
    private static final Logger log = LoggerFactory.getLogger(PermApprovalService.class);
    private final PermApprovalRepository permApprovalRepository;

    public List<PermApprovalRes> listByApplicant(String applicantId) {
        return this.permApprovalRepository.findByApplicantIdOrderByCreateTimeDesc(applicantId).stream().map(this::toRes).collect(Collectors.toList());
    }

    public List<PermApprovalRes> listPending() {
        return this.permApprovalRepository.findByStatusOrderByCreateTimeDesc("PENDING").stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public PermApprovalRes create(PermApprovalReq req, String applicantId, String applicantName, String tenantId) {
        PermApprovalEntity entity = new PermApprovalEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApplicantId(applicantId);
        entity.setApplicantName(applicantName);
        entity.setResourceType(req.getResourceType());
        entity.setResourceId(req.getResourceId());
        entity.setResourceName(req.getResourceName());
        entity.setAccessType(req.getAccessType());
        entity.setReason(req.getReason());
        entity.setExpireTime(req.getExpireTime());
        entity.setStatus("PENDING");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((PermApprovalEntity)this.permApprovalRepository.save(entity));
    }

    @Transactional
    public PermApprovalRes approve(String id, String approverId, String approverName, String comment) {
        PermApprovalEntity entity = (PermApprovalEntity)this.permApprovalRepository.findById(id).orElseThrow(() -> new BusinessException("Approval record not found"));
        entity.setStatus("APPROVED");
        entity.setApproverId(approverId);
        entity.setApproverName(approverName);
        entity.setApproveComment(comment);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((PermApprovalEntity)this.permApprovalRepository.save(entity));
    }

    @Transactional
    public PermApprovalRes reject(String id, String approverId, String approverName, String comment) {
        PermApprovalEntity entity = (PermApprovalEntity)this.permApprovalRepository.findById(id).orElseThrow(() -> new BusinessException("Approval record not found"));
        entity.setStatus("REJECTED");
        entity.setApproverId(approverId);
        entity.setApproverName(approverName);
        entity.setApproveComment(comment);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((PermApprovalEntity)this.permApprovalRepository.save(entity));
    }

    private PermApprovalRes toRes(PermApprovalEntity e) {
        PermApprovalRes res = new PermApprovalRes();
        res.setId(e.getId());
        res.setApplicantId(e.getApplicantId());
        res.setApplicantName(e.getApplicantName());
        res.setResourceType(e.getResourceType());
        res.setResourceId(e.getResourceId());
        res.setResourceName(e.getResourceName());
        res.setAccessType(e.getAccessType());
        res.setReason(e.getReason());
        res.setStatus(e.getStatus());
        res.setApproverId(e.getApproverId());
        res.setApproverName(e.getApproverName());
        res.setApproveComment(e.getApproveComment());
        res.setExpireTime(e.getExpireTime() != null ? e.getExpireTime().toString() : null);
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }

    public PermApprovalService(PermApprovalRepository permApprovalRepository) {
        this.permApprovalRepository = permApprovalRepository;
    }
}

