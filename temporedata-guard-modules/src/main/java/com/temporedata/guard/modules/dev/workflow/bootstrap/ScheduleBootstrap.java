package com.temporedata.guard.modules.dev.workflow.bootstrap;

import com.temporedata.guard.modules.dev.workflow.service.QuartzScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Bootstrap: re-register all active Quartz schedules on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleBootstrap implements CommandLineRunner {

    private final QuartzScheduleService scheduleService;

    @Override
    public void run(String... args) {
        log.info("Re-registering all active Quartz schedules...");
        scheduleService.registerAllSchedules();
        log.info("Quartz schedules re-registration complete");
    }
}