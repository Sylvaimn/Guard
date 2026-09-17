package com.temporedata.guard.modules.common.work.service;
import com.temporedata.guard.modules.common.work.entity.WorkEntity;
import com.temporedata.guard.modules.common.work.entity.WorkInstanceEntity;
import com.temporedata.guard.modules.common.work.repository.WorkInstanceRepository;
import com.temporedata.guard.modules.common.work.repository.WorkRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Independent job management service.
 * Supports CRUD, run, stop, status check, log retrieval for single jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final WorkInstanceRepository workInstanceRepository;

    public WorkEntity addWork(String name, String workType, String workflowId, String tenantId, String createBy) {
        WorkEntity work = new WorkEntity();
        work.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        work.setName(name);
        work.setWorkType(workType);
        work.setWorkflowId(workflowId);
        work.setStatus("DRAFT");
        work.setTenantId(tenantId);
        work.setCreateBy(createBy);
        work.setCreateDateTime(LocalDateTime.now());
        work.setLastModifiedDateTime(LocalDateTime.now());
        return workRepository.save(work);
    }

    public WorkEntity updateWork(String workId, String name, String remark, String config) {
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
        if (name != null) work.setName(name);
        if (remark != null) work.setRemark(remark);
        if (config != null) work.setConfig(config);
        work.setLastModifiedDateTime(LocalDateTime.now());
        return workRepository.save(work);
    }

    @Transactional
    public void deleteWork(String workId) {
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
        if ("RUNNING".equals(work.getStatus())) {
            throw new BusinessException("运行中的作业不能删除");
        }
        workRepository.delete(work);
    }

    @Async
    @Transactional
    public WorkInstanceEntity runWork(String workId) {
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));

        work.setStatus("RUNNING");
        workRepository.save(work);

        WorkInstanceEntity instance = new WorkInstanceEntity();
        instance.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        instance.setWorkId(workId);
        instance.setStatus("RUNNING");
        instance.setTenantId(work.getTenantId());
        instance.setExecStartDateTime(LocalDateTime.now());
        instance.setCreateDateTime(LocalDateTime.now());
        instance = workInstanceRepository.save(instance);

        try {
            // Execute based on work type
            executeWork(work, instance);

            instance.setStatus("SUCCESS");
            instance.setExecEndDateTime(LocalDateTime.now());
            instance.setDuration(java.time.Duration.between(instance.getExecStartDateTime(), instance.getExecEndDateTime()).toMillis());
            work.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("Work execution failed: {}", workId, e);
            instance.setStatus("FAIL");
            instance.setSubmitLog(e.getMessage());
            instance.setExecEndDateTime(LocalDateTime.now());
            instance.setDuration(java.time.Duration.between(instance.getExecStartDateTime(), instance.getExecEndDateTime()).toMillis());
            work.setStatus("FAIL");
        }

        work.setLastModifiedDateTime(LocalDateTime.now());
        workRepository.save(work);
        return workInstanceRepository.save(instance);
    }

    private void executeWork(WorkEntity work, WorkInstanceEntity instance) throws Exception {
        String config = work.getConfig();
        switch (work.getWorkType()) {
            case "SPARK_SQL":
                // Execute via SparkSQL executor
                break;
            case "BASH":
                // Execute via Bash executor
                break;
            case "PYTHON":
                // Execute via Python executor
                break;
            case "CURL":
                // Execute via Curl executor
                break;
            case "API":
                // Execute via API executor
                break;
            default:
                log.info("Work type {} executed with config: {}", work.getWorkType(), config);
        }
        // Simulate execution
        Thread.sleep(1000);
        instance.setSubmitLog("Job submitted successfully");
        instance.setResultData("{\"status\": \"completed\"}");
    }

    @Transactional
    public void stopWork(String workId) {
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
        if (!"RUNNING".equals(work.getStatus())) {
            throw new BusinessException("作业未在运行中");
        }
        work.setStatus("STOPPED");
        work.setLastModifiedDateTime(LocalDateTime.now());
        workRepository.save(work);

        // Update latest running instance
        List<WorkInstanceEntity> instances = workInstanceRepository.findByWorkIdOrderByCreateDateTimeDesc(workId);
        if (!instances.isEmpty() && "RUNNING".equals(instances.get(0).getStatus())) {
            WorkInstanceEntity instance = instances.get(0);
            instance.setStatus("STOPPED");
            instance.setExecEndDateTime(LocalDateTime.now());
            instance.setDuration(java.time.Duration.between(instance.getExecStartDateTime(), instance.getExecEndDateTime()).toMillis());
            workInstanceRepository.save(instance);
        }
    }

    public WorkEntity getWork(String workId) {
        return workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
    }

    public Page<WorkEntity> pageWork(String workflowId, int page, int pageSize) {
        if (workflowId != null) {
            List<WorkEntity> list = workRepository.findByWorkflowId(workflowId);
            int start = page * pageSize;
            int end = Math.min(start + pageSize, list.size());
            return new org.springframework.data.domain.PageImpl<>(
                    list.subList(Math.min(start, list.size()), end),
                    PageRequest.of(page, pageSize),
                    list.size());
        }
        return workRepository.findAll(PageRequest.of(page, pageSize));
    }

    public List<WorkEntity> listByWorkflow(String workflowId) {
        return workRepository.findByWorkflowIdOrderByTopIndexAsc(workflowId);
    }

    public WorkEntity copyWork(String workId) {
        WorkEntity source = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
        WorkEntity copy = new WorkEntity();
        copy.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        copy.setName(source.getName() + " (副本)");
        copy.setWorkType(source.getWorkType());
        copy.setRemark(source.getRemark());
        copy.setConfig(source.getConfig());
        copy.setWorkflowId(source.getWorkflowId());
        copy.setStatus("DRAFT");
        copy.setTenantId(source.getTenantId());
        copy.setCreateBy(source.getCreateBy());
        copy.setCreateDateTime(LocalDateTime.now());
        copy.setLastModifiedDateTime(LocalDateTime.now());
        return workRepository.save(copy);
    }

    @Transactional
    public void topWork(String workId) {
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
        work.setTopIndex(0);
        workRepository.save(work);
    }

    public List<WorkInstanceEntity> queryInstance(String workId) {
        return workInstanceRepository.findByWorkIdOrderByCreateDateTimeDesc(workId);
    }

    public Map<String, Object> getStatus(String workId) {
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new BusinessException("作业不存在"));
        Map<String, Object> status = new HashMap<>();
        status.put("workId", workId);
        status.put("status", work.getStatus());
        List<WorkInstanceEntity> instances = workInstanceRepository.findByWorkIdOrderByCreateDateTimeDesc(workId);
        if (!instances.isEmpty()) {
            status.put("latestInstance", instances.get(0));
        }
        return status;
    }
}