package com.temporedata.guard.modules.dev.sync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时注册增量同步任务的定时轮询。
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class SyncSchedulerBootstrap implements CommandLineRunner {

    private final SyncSchedulerService syncSchedulerService;

    @Override
    public void run(String... args) {
        try {
            syncSchedulerService.registerAll();
        } catch (Exception e) {
            log.warn("Sync scheduler bootstrap failed: {}", e.getMessage());
        }
    }
}
