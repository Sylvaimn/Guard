/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiMemoryEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AiMemoryRepository;



import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemoryService {
    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    private final AiMemoryRepository memoryRepository;
    private static final int DEFAULT_TTL_DAYS = 30;

    @Transactional
    public AiMemoryEntity remember(String userId, String tenantId, String type, String key, String content, Integer ttlDays) {
        this.memoryRepository.findFirstByUserIdAndTenantIdAndKeyOrderByAccessTimeDesc(userId, tenantId, key).ifPresent(existing -> {
            existing.setContent(content);
            existing.setType(type);
            existing.setAccessTime(LocalDateTime.now());
            existing.setExpireTime(ttlDays == null ? LocalDateTime.now().plusDays(30L) : LocalDateTime.now().plusDays(ttlDays.intValue()));
            this.memoryRepository.save(existing);
        });
        if (this.memoryRepository.findFirstByUserIdAndTenantIdAndKeyOrderByAccessTimeDesc(userId, tenantId, key).isEmpty()) {
            AiMemoryEntity m = new AiMemoryEntity();
            m.setId(UUID.randomUUID().toString().replace("-", ""));
            m.setUserId(userId);
            m.setTenantId(tenantId);
            m.setType(type);
            m.setKey(key);
            m.setContent(content);
            m.setAccessTime(LocalDateTime.now());
            m.setCreateTime(LocalDateTime.now());
            m.setExpireTime(ttlDays == null ? LocalDateTime.now().plusDays(30L) : LocalDateTime.now().plusDays(ttlDays.intValue()));
            this.memoryRepository.save(m);
        }
        return this.memoryRepository.findFirstByUserIdAndTenantIdAndKeyOrderByAccessTimeDesc(userId, tenantId, key).orElse(null);
    }

    public List<AiMemoryEntity> recall(String userId, String tenantId, String type) {
        return this.memoryRepository.findByUserIdAndTenantIdAndTypeOrderByAccessTimeDesc(userId, tenantId, type).stream().filter(m -> m.getExpireTime() == null || m.getExpireTime().isAfter(LocalDateTime.now())).limit(20L).collect(Collectors.toList());
    }

    @Transactional
    public String recallByKey(String userId, String tenantId, String key) {
        return this.memoryRepository.findFirstByUserIdAndTenantIdAndKeyOrderByAccessTimeDesc(userId, tenantId, key).map(m -> {
            m.setAccessTime(LocalDateTime.now());
            this.memoryRepository.save(m);
            return m.getContent();
        }).orElse(null);
    }

    @Transactional
    public void forget(String userId, String tenantId, String key) {
        this.memoryRepository.findFirstByUserIdAndTenantIdAndKeyOrderByAccessTimeDesc(userId, tenantId, key).ifPresent(arg_0 -> ((AiMemoryRepository)this.memoryRepository).delete(arg_0));
    }

    public MemoryService(AiMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }
}

