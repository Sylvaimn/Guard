package com.temporedata.guard.modules.common.async;

import com.temporedata.guard.common.locker.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务执行器：HTTP 请求线程只提交任务立即返回，长任务在独立线程池执行。
 * 同一任务键单飞（single-flight）：基于 DistributedLock（单机内存锁，可升级 Redis 锁）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutorService {

    private final Executor taskExecutor;
    private final DistributedLock distributedLock;

    /**
     * 提交任务并立即返回。同 key 任务已在执行时返回 false。
     */
    public boolean submit(String key, Runnable task) {
        if (!distributedLock.tryLock(key, 1000)) {
            log.warn("Task already running, rejected: {}", key);
            return false;
        }
        try {
            taskExecutor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("Async task failed: {}", key, e);
                } finally {
                    distributedLock.unlock(key);
                }
            });
            return true;
        } catch (RuntimeException e) {
            distributedLock.unlock(key);
            throw e;
        }
    }

    @Configuration
    public static class TaskExecutorConfig {

        @Bean(name = "taskExecutor")
        public Executor taskExecutor(
                @Value("${zy.task.pool.core-size:4}") int coreSize,
                @Value("${zy.task.pool.max-size:8}") int maxSize,
                @Value("${zy.task.pool.queue-capacity:2000}") int queueCapacity) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(coreSize);
            executor.setMaxPoolSize(maxSize);
            executor.setQueueCapacity(queueCapacity);
            executor.setThreadNamePrefix("tdg-task-");
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.initialize();
            return executor;
        }
    }
}
