/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiTaskEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AiTaskRepository;
import com.temporedata.guard.modules.agent.aianalysis.ws.AiTaskWsHub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;



import com.temporedata.guard.modules.common.async.TaskExecutorService;
import com.temporedata.guard.modules.common.eventbus.EventBus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiTaskService {
    private static final Logger log = LoggerFactory.getLogger(AiTaskService.class);
    private final AiTaskRepository taskRepository;
    private final TaskExecutorService taskExecutorService;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Function<Map<String, Object>, String>> handlers = new ConcurrentHashMap<String, Function<Map<String, Object>, String>>();

    public void registerHandler(String type, Function<Map<String, Object>, String> handler) {
        this.handlers.put(type, handler);
    }

    public AiTaskEntity submit(String type, Map<String, Object> payload, Integer priority, String userId, String tenantId) {
        if (!this.handlers.containsKey(type)) {
            throw new BusinessException("\u672a\u6ce8\u518c\u7684\u4efb\u52a1\u7c7b\u578b: " + type);
        }
        AiTaskEntity task = new AiTaskEntity();
        task.setId(UUID.randomUUID().toString().replace("-", ""));
        task.setType(type);
        try {
            task.setPayload(this.objectMapper.writeValueAsString(payload == null ? Map.of() : payload));
        }
        catch (Exception e) {
            task.setPayload("{}");
        }
        task.setPriority(priority == null ? 5 : Math.max(0, Math.min(9, priority)));
        task.setUserId(userId);
        task.setStatus("READY");
        task.setTenantId(tenantId);
        task.setCreateTime(LocalDateTime.now());
        AiTaskEntity saved = (AiTaskEntity)this.taskRepository.save(task);
        this.taskExecutorService.submit("aitask:" + saved.getId(), () -> this.execute(saved.getId()));
        return saved;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void execute(String taskId) {
        AiTaskEntity task = this.taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        this.taskRepository.save(task);
        try {
            Map payload;
            Function<Map<String, Object>, String> handler = this.handlers.get(task.getType());
            if (handler == null) {
                throw new BusinessException("\u672a\u6ce8\u518c\u7684\u4efb\u52a1\u7c7b\u578b: " + task.getType());
            }
            try {
                payload = (Map)this.objectMapper.readValue(task.getPayload() == null ? "{}" : task.getPayload(), Map.class);
            }
            catch (Exception e) {
                payload = Map.of();
            }
            Map<String, Object> ctx = new HashMap<String, Object>(payload);
            ctx.put("tenantId", task.getTenantId());
            String resultRef = handler.apply(ctx);
            task.setStatus("SUCCESS");
            task.setResultRef(resultRef);
            log.info("[AI-TASK] {} {} -> SUCCESS", (Object)task.getType(), (Object)taskId);
        }
        catch (Exception e) {
            log.error("[AI-TASK] {} {} failed: {}", new Object[]{task.getType(), taskId, e.getMessage()});
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage() == null ? "\u672a\u77e5\u9519\u8bef" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
        }
        finally {
            task.setFinishTime(LocalDateTime.now());
            this.taskRepository.save(task);
            AiTaskWsHub.publish(task.getUserId(), task.getTenantId(), task.getId(), task.getStatus(), task.getResultRef());
            this.eventBus.publish("tdg-task", "TASK_FINISHED", Map.of("taskId", task.getId(), "type", task.getType(), "status", task.getStatus(), "userId", task.getUserId(), "tenantId", task.getTenantId()));
        }
    }

    public AiTaskEntity detail(String id, String tenantId) {
        AiTaskEntity task = (AiTaskEntity)this.taskRepository.findById(id).orElseThrow(() -> new BusinessException("\u4efb\u52a1\u4e0d\u5b58\u5728"));
        if (!task.getTenantId().equals(tenantId)) {
            throw new BusinessException("\u65e0\u6743\u8bbf\u95ee\u8be5\u4efb\u52a1");
        }
        return task;
    }

    public List<AiTaskEntity> list(String tenantId, String status) {
        return status != null && !status.isBlank() ? this.taskRepository.findByTenantIdAndStatusOrderByCreateTimeDesc(tenantId, status) : this.taskRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    public AiTaskService(AiTaskRepository taskRepository, TaskExecutorService taskExecutorService, EventBus eventBus) {
        this.taskRepository = taskRepository;
        this.taskExecutorService = taskExecutorService;
        this.eventBus = eventBus;
    }
}

