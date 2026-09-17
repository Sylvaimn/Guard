package com.temporedata.guard.modules.dev.real.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.real.RealJobReq;
import com.temporedata.guard.api.real.RealJobRes;
import com.temporedata.guard.modules.dev.real.entity.RealJobEntity;
import com.temporedata.guard.modules.dev.real.repository.RealJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Realtime computing job management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealService {

    private final RealJobRepository realJobRepository;

    public List<RealJobRes> list(String tenantId) {
        return realJobRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public RealJobRes detail(String id) {
        return toRes(realJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("实时作业不存在")));
    }

    @Transactional
    public RealJobRes create(RealJobReq req, String tenantId) {
        RealJobEntity entity = new RealJobEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setScript(req.getScript());
        entity.setClusterId(req.getClusterId());
        entity.setStatus("STOPPED");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(realJobRepository.save(entity));
    }

    @Transactional
    public RealJobRes update(String id, RealJobReq req) {
        RealJobEntity entity = realJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("实时作业不存在"));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setScript(req.getScript());
        entity.setClusterId(req.getClusterId());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(realJobRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        realJobRepository.deleteById(id);
    }

    @Transactional
    public void startJob(String id) {
        RealJobEntity entity = realJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("实时作业不存在"));
        if ("RUNNING".equals(entity.getStatus())) {
            throw new BusinessException("作业已在运行中");
        }
        entity.setStatus("RUNNING");
        entity.setUpdateTime(LocalDateTime.now());
        realJobRepository.save(entity);
        log.info("Realtime job {} started", id);
    }

    @Transactional
    public void stopJob(String id) {
        RealJobEntity entity = realJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("实时作业不存在"));
        if (!"RUNNING".equals(entity.getStatus())) {
            throw new BusinessException("作业未在运行中");
        }
        entity.setStatus("STOPPED");
        entity.setUpdateTime(LocalDateTime.now());
        realJobRepository.save(entity);
        log.info("Realtime job {} stopped", id);
    }

    @Transactional
    public String triggerSavepoint(String id) {
        RealJobEntity entity = realJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("实时作业不存在"));
        if (!"RUNNING".equals(entity.getStatus())) {
            throw new BusinessException("作业未在运行中，无法触发 Savepoint");
        }
        String savepointPath = "/checkpoints/" + id + "/savepoint-" + System.currentTimeMillis();
        entity.setSavepointPath(savepointPath);
        entity.setUpdateTime(LocalDateTime.now());
        realJobRepository.save(entity);
        log.info("Savepoint triggered for job {}, path: {}", id, savepointPath);
        return savepointPath;
    }

    public String getLogs(String id) {
        realJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("实时作业不存在"));
        return "No logs available yet for job " + id;
    }

    private RealJobRes toRes(RealJobEntity e) {
        RealJobRes res = new RealJobRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setStatus(e.getStatus());
        res.setClusterId(e.getClusterId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }
}