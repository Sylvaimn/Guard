package com.temporedata.guard.modules.dev.engine.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.dev.engine.entity.ResourceQuotaEntity;
import com.temporedata.guard.modules.dev.engine.entity.SparkJobEntity;
import com.temporedata.guard.modules.dev.engine.repository.ResourceQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 资源配额服务: 租户级作业资源限额(Executor 数/内存/时长/并发), 作业提交前强制校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceQuotaService {

    private final ResourceQuotaRepository quotaRepository;

    /** 获取配额(默认值兜底) */
    public ResourceQuotaEntity getQuota(String tenantId) {
        return quotaRepository.findById(tenantId)
                .orElseGet(() -> {
                    ResourceQuotaEntity q = new ResourceQuotaEntity();
                    q.setTenantId(tenantId);
                    return q;
                });
    }

    /** 更新配额 */
    public ResourceQuotaEntity updateQuota(String tenantId, Integer maxExecutors, Integer maxMemoryGb,
                                           Integer maxRuntimeMin, Integer maxConcurrency) {
        ResourceQuotaEntity q = quotaRepository.findById(tenantId)
                .orElseGet(() -> {
                    ResourceQuotaEntity n = new ResourceQuotaEntity();
                    n.setTenantId(tenantId);
                    return n;
                });
        if (maxExecutors != null) q.setMaxExecutors(maxExecutors);
        if (maxMemoryGb != null) q.setMaxMemoryGb(maxMemoryGb);
        if (maxRuntimeMin != null) q.setMaxRuntimeMin(maxRuntimeMin);
        if (maxConcurrency != null) q.setMaxConcurrency(maxConcurrency);
        q.setUpdateTime(LocalDateTime.now());
        return quotaRepository.save(q);
    }

    /** 作业提交前校验配额 */
    public void validate(SparkJobEntity job) {
        ResourceQuotaEntity q = getQuota(job.getTenantId());

        // Executor 数校验
        int executors = job.getNumExecutors() == null ? 1 : job.getNumExecutors();
        if (executors > q.getMaxExecutors()) {
            throw new BusinessException(String.format(
                    "资源配额超限: Executor %d > 上限 %d (租户 %s)", executors, q.getMaxExecutors(), job.getTenantId()));
        }
        // 内存校验(executor + driver, GB)
        int mem = parseMemoryGb(job.getExecutorMemory()) + parseMemoryGb(job.getDriverMemory());
        if (mem > q.getMaxMemoryGb()) {
            throw new BusinessException(String.format(
                    "资源配额超限: 内存 %dG > 上限 %dG (租户 %s)", mem, q.getMaxMemoryGb(), job.getTenantId()));
        }
        log.info("[QUOTA] 作业 {} 通过配额校验(executors={}, mem={}G)", job.getName(), executors, mem);
    }

    /** 内存串解析为 GB: "4g"/"2048m" -> 数值 */
    private int parseMemoryGb(String mem) {
        if (mem == null || mem.isBlank()) return 1;
        String v = mem.trim().toLowerCase();
        try {
            if (v.endsWith("g")) return (int) Double.parseDouble(v.substring(0, v.length() - 1));
            if (v.endsWith("m")) return (int) Math.ceil(Double.parseDouble(v.substring(0, v.length() - 1)) / 1024.0);
            return (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    

    /** 配额展示 */
    public Map<String, Object> quotaView(String tenantId) {
        ResourceQuotaEntity q = getQuota(tenantId);
        return Map.of(
                "tenantId", q.getTenantId(),
                "maxExecutors", q.getMaxExecutors(),
                "maxMemoryGb", q.getMaxMemoryGb(),
                "maxRuntimeMin", q.getMaxRuntimeMin(),
                "maxConcurrency", q.getMaxConcurrency(),
                "updateTime", q.getUpdateTime() == null ? null : q.getUpdateTime().toString());
    }
}
