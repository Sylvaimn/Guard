package com.temporedata.guard.modules.dev.lineage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 热查询缓存（双模式）：
 * - Redis 可用: 走 Redis string + TTL(5分钟), 多节点共享缓存
 * - Redis 不可用: 回退进程内 ConcurrentHashMap TTL, 保证单机可用
 * Keys 按租户隔离；重建/增量采集后 clear 防脏读。
 */
@Slf4j
@Component
public class LineageCacheService {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final String KEY_PREFIX = "zy:cache:lineage:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    public LineageCacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public <T> T get(String tenantId, String method, String params, Class<T> type, Supplier<T> loader) {
        String key = KEY_PREFIX + tenantId + "|" + method + "|" + params;
        StringRedisTemplate redis = getRedis();
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(key);
                if (json != null) {
                    return objectMapper.readValue(json, type);
                }
                T value = loader.get();
                redis.opsForValue().set(key, objectMapper.writeValueAsString(value), TTL_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                return value;
            } catch (Exception e) {
                log.warn("Redis cache failed, fallback to local: {}", e.getMessage());
                return localGet(key, loader);
            }
        }
        return localGet(key, loader);
    }

    public void clear(String tenantId) {
        String prefix = KEY_PREFIX + tenantId;
        StringRedisTemplate redis = getRedis();
        if (redis != null) {
            try {
                // 扫描删除该租户缓存(生产建议改 SCAN; 本地键量小可 KEYS)
                java.util.Set<String> keys = redis.keys(prefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
            } catch (Exception e) {
                log.debug("Redis cache clear failed, local clear anyway: {}", e.getMessage());
            }
        }
        localCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clearAll() {
        localCache.clear();
        StringRedisTemplate redis = getRedis();
        if (redis != null) {
            try {
                java.util.Set<String> keys = redis.keys(KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
            } catch (Exception e) {
                log.debug("Redis clearAll failed: {}", e.getMessage());
            }
        }
    }

    private <T> T localGet(String key, Supplier<T> loader) {
        CacheEntry entry = localCache.get(key);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expireAt > now) {
            return (T) entry.value;
        }
        T value = loader.get();
        localCache.put(key, new CacheEntry(value, now + TTL_MS));
        return value;
    }

    private StringRedisTemplate getRedis() {
        try {
            return redisTemplateProvider.getIfAvailable();
        } catch (Exception e) {
            return null;
        }
    }

    private static class CacheEntry {
        final Object value;
        final long expireAt;

        CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
