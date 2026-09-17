package com.temporedata.guard.modules.data.report.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.report.ReportReq;
import com.temporedata.guard.api.report.ReportRes;
import com.temporedata.guard.modules.data.report.entity.ReportEntity;
import com.temporedata.guard.modules.data.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data report service: CRUD, publish and unpublish.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    public List<ReportRes> listAll(String tenantId) {
        return reportRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReportRes create(ReportReq req, String tenantId) {
        ReportEntity entity = new ReportEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setType(req.getType());
        entity.setConfig(req.getConfig());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setSecretLevel(req.getSecretLevel());
        entity.setCaliberDesc(req.getCaliberDesc());
        entity.setSchedulePeriod(req.getSchedulePeriod());
        entity.setStatus("DRAFT");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(reportRepository.save(entity));
    }

    @Transactional
    public ReportRes update(String id, ReportReq req) {
        ReportEntity entity = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报表不存在"));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setType(req.getType());
        entity.setConfig(req.getConfig());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setSecretLevel(req.getSecretLevel());
        entity.setCaliberDesc(req.getCaliberDesc());
        entity.setSchedulePeriod(req.getSchedulePeriod());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(reportRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        reportRepository.deleteById(id);
    }

    @Transactional
    public ReportRes publish(String id) {
        ReportEntity entity = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报表不存在"));
        entity.setStatus("PUBLISHED");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(reportRepository.save(entity));
    }

    @Transactional
    public ReportRes unpublish(String id) {
        ReportEntity entity = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报表不存在"));
        entity.setStatus("DRAFT");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(reportRepository.save(entity));
    }

    private ReportRes toRes(ReportEntity e) {
        ReportRes res = new ReportRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setDescription(e.getDescription());
        res.setType(e.getType());
        res.setConfig(e.getConfig());
        res.setDatasourceId(e.getDatasourceId());
        res.setStatus(e.getStatus());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        res.setSecretLevel(e.getSecretLevel());
        res.setCaliberDesc(e.getCaliberDesc());
        res.setSchedulePeriod(e.getSchedulePeriod());
        return res;
    }
}