package com.temporedata.guard.modules.dev.quality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时注册质量规则定时检查。
 */
@Slf4j
@Component
@Order(21)
@RequiredArgsConstructor
public class QualityScheduleBootstrap implements CommandLineRunner {

    private final QualityScheduleService qualityScheduleService;

    @Override
    public void run(String... args) {
        try {
            qualityScheduleService.registerAll();
        } catch (Exception e) {
            log.warn("Quality scheduler bootstrap failed: {}", e.getMessage());
        }
    }
}
