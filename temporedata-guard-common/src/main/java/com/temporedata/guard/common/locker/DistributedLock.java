package com.temporedata.guard.common.locker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁（双模式）：
 * - 单节点：JVM Semaphore（跨线程可释放：提交线程获取、执行线程释放）
 * - 多节点：若检测到 Redis（spring-data-redis 自动装配），升级为 SETNX + TTL 分布式锁，
 *   同一任务键跨节点单飞；Redis 不可用时自动回退本地锁。
 */
@Slf4j
@Component
public class DistributedLock {

    private final ConcurrentHashMap<String, Semaphore> localLocks = new ConcurrentHashMap<>();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final long lockTtlSeconds;
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    public DistributedLock(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                           @Value("${zy.lock.ttl-seconds:14400}") long lockTtlSeconds) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.lockTtlSeconds = lockTtlSeconds;
    }

    /**
     * Try to acquire lock within timeout.
     *
     * @param key       lock key
     * @param timeoutMs timeout in milliseconds
     * @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String key, long timeoutMs) {
        // 1. 本地信号量（同 JVM 内互斥；Semaphore 不绑定线程，允许提交线程获取、执行线程释放）
        Semaphore semaphore = localLocks.computeIfAbsent(key, k -> new Semaphore(1));
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!acquired) {
            log.warn("Lock acquisition timed out: {}", key);
            return false;
        }

        // 2. 跨节点声明（Redis 可用时；失败自动回退本地锁）
        StringRedisTemplate redis = getRedisIfUsable();
        if (redis != null) {
            try {
                Boolean claimed = redis.opsForValue().setIfAbsent("zy:lock:" + key, instanceId, lockTtlSeconds, TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(claimed)) {
                    // 被其他节点持有 → 释放本地信号量
                    semaphore.release();
                    log.warn("Lock held by another node: {}", key);
                    return false;
                }
            } catch (Exception e) {
                log.warn("Redis lock unavailable, fallback to local lock: {}", e.getMessage());
                // 保留本地锁，继续执行
            }
        }
        log.debug("Lock acquired: {}", key);
        return true;
    }

    /**
     * Release the lock.
     */
    public void unlock(String key) {
        StringRedisTemplate redis = getRedisIfUsable();
        if (redis != null) {
            try {
                // 仅当锁属主是当前实例时释放，防止误删其他节点锁
                String owner = redis.opsForValue().get("zy:lock:" + key);
                if (instanceId.equals(owner)) {
                    redis.delete("zy:lock:" + key);
                }
            } catch (Exception e) {
                log.debug("Redis unlock failed (local unlock continues): {}", e.getMessage());
            }
        }
        Semaphore semaphore = localLocks.get(key);
        if (semaphore != null) {
            semaphore.release();
            log.debug("Lock released: {}", key);
            if (semaphore.getQueueLength() == 0 && semaphore.availablePermits() > 0) {
                localLocks.remove(key, semaphore);
            }
        }
    }

    private StringRedisTemplate getRedisIfUsable() {
        try {
            return redisTemplateProvider.getIfAvailable();
        } catch (Exception e) {
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        localLocks.clear();
    }
}
