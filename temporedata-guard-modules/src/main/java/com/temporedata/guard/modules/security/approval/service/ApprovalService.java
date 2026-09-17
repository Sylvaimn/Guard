/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.approval.ApprovalReq
 *  com.temporedata.guard.api.approval.ApprovalRes
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.approval.service;
import com.temporedata.guard.modules.security.approval.entity.ApprovalEntity;
import com.temporedata.guard.modules.security.approval.repository.ApprovalRepository;

import com.temporedata.guard.api.approval.ApprovalReq;
import com.temporedata.guard.api.approval.ApprovalRes;
import com.temporedata.guard.api.base.exceptions.BusinessException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    private final ApprovalRepository approvalRepository;

    public List<ApprovalRes> listAll(String tenantId) {
        return this.approvalRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public ApprovalRes create(ApprovalReq req, String tenantId) {
        ApprovalEntity entity = new ApprovalEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setWorkflowId(req.getWorkflowId());
        entity.setTitle(req.getTitle());
        entity.setDescription(req.getDescription());
        entity.setStatus("PENDING");
        entity.setApplicantId(req.getApplicantId());
        entity.setApproverId(req.getApproverId());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((ApprovalEntity)this.approvalRepository.save(entity));
    }

    @Transactional
    public ApprovalRes update(String id, ApprovalReq req) {
        ApprovalEntity entity = (ApprovalEntity)this.approvalRepository.findById(id).orElseThrow(() -> new BusinessException("\u5ba1\u6279\u4e0d\u5b58\u5728"));
        entity.setWorkflowId(req.getWorkflowId());
        entity.setTitle(req.getTitle());
        entity.setDescription(req.getDescription());
        entity.setApplicantId(req.getApplicantId());
        entity.setApproverId(req.getApproverId());
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((ApprovalEntity)this.approvalRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        this.approvalRepository.deleteById(id);
    }

    @Transactional
    public ApprovalRes approve(String id, String comment) {
        ApprovalEntity entity = (ApprovalEntity)this.approvalRepository.findById(id).orElseThrow(() -> new BusinessException("\u5ba1\u6279\u4e0d\u5b58\u5728"));
        entity.setStatus("APPROVED");
        entity.setComment(comment);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((ApprovalEntity)this.approvalRepository.save(entity));
    }

    @Transactional
    public ApprovalRes reject(String id, String comment) {
        ApprovalEntity entity = (ApprovalEntity)this.approvalRepository.findById(id).orElseThrow(() -> new BusinessException("\u5ba1\u6279\u4e0d\u5b58\u5728"));
        entity.setStatus("REJECTED");
        entity.setComment(comment);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((ApprovalEntity)this.approvalRepository.save(entity));
    }

    private ApprovalRes toRes(ApprovalEntity e) {
        ApprovalRes res = new ApprovalRes();
        res.setId(e.getId());
        res.setWorkflowId(e.getWorkflowId());
        res.setTitle(e.getTitle());
        res.setDescription(e.getDescription());
        res.setStatus(e.getStatus());
        res.setApplicantId(e.getApplicantId());
        res.setApproverId(e.getApproverId());
        res.setComment(e.getComment());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }

    public ApprovalService(ApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }
}

