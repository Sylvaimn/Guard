/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.ObjectProvider
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.vector.VectorStore;


import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SemanticCacheService {
    private static final Logger log = LoggerFactory.getLogger(SemanticCacheService.class);
    private static final double SIM_THRESHOLD = 0.92;
    private static final long TTL_SECONDS = 300L;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final Map<String, List<String>> queryIndex = new ConcurrentHashMap<String, List<String>>();

    public SemanticCacheService(ObjectProvider<StringRedisTemplate> redisProvider, ObjectProvider<VectorStore> vectorStoreProvider) {
        this.redisProvider = redisProvider;
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public String get(String tenantId, String authFingerprint, String query) {
        List<String> near;
        StringRedisTemplate redis = this.redis();
        if (redis == null) {
            return null;
        }
        String exactKey = this.cacheKey(tenantId, authFingerprint, query);
        String exact = (String)redis.opsForValue().get((Object)exactKey);
        if (exact != null) {
            return exact;
        }
        VectorStore vs = (VectorStore)this.vectorStoreProvider.getIfAvailable();
        if (vs != null && vs.available() && !(near = vs.searchEntityIds(query, 3)).isEmpty()) {
            String semanticKey = this.cacheKey(tenantId, authFingerprint, "sem:" + (near.isEmpty() ? query : near.get(0)));
            String hit = (String)redis.opsForValue().get((Object)semanticKey);
            if (hit != null) {
                log.info("[SEM-CACHE] semantic hit: {} ~ {}", (Object)query, (Object)near.get(0));
                return hit;
            }
        }
        return null;
    }

    public void put(String tenantId, String authFingerprint, String query, String result) {
        StringRedisTemplate redis = this.redis();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(this.cacheKey(tenantId, authFingerprint, query), result, 300L, TimeUnit.SECONDS);
            VectorStore vs = (VectorStore)this.vectorStoreProvider.getIfAvailable();
            if (vs != null && vs.available()) {
                EntityMasterEntity pseudo = new EntityMasterEntity();
                pseudo.setId("sem:" + Integer.toHexString(query.hashCode()));
                pseudo.setDisplayName(query);
                pseudo.setTenantId(tenantId);
                pseudo.setDensity("PUBLIC");
                vs.upsertEntity(pseudo, List.of());
                redis.opsForValue().set(this.cacheKey(tenantId, authFingerprint, "sem:" + pseudo.getId()), result, 300L, TimeUnit.SECONDS);
            }
        }
        catch (Exception e) {
            log.debug("semantic cache put skipped: {}", (Object)e.getMessage());
        }
    }

    public void invalidateTenant(String tenantId) {
        StringRedisTemplate redis = this.redis();
        if (redis == null) {
            return;
        }
        try {
            Set<String> keys = redis.keys("zy:sem:" + tenantId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        }
        catch (Exception e) {
            log.debug("semantic cache invalidate failed: {}", (Object)e.getMessage());
        }
        this.queryIndex.remove(tenantId);
    }

    private String cacheKey(String tenantId, String authFingerprint, String query) {
        return "zy:sem:" + tenantId + ":" + authFingerprint + ":" + Integer.toHexString(query.hashCode());
    }

    private StringRedisTemplate redis() {
        try {
            return (StringRedisTemplate)this.redisProvider.getIfAvailable();
        }
        catch (Exception e) {
            return null;
        }
    }
}

