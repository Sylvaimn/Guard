package com.temporedata.guard.modules.dev.indicator.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.dev.indicator.entity.IndicatorEntity;
import com.temporedata.guard.modules.dev.indicator.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Quartz-based indicator schedule management.
 * Registers one Quartz job per indicator with an enabled cron schedule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorSchedulerService {

    private final Scheduler scheduler;
    private final IndicatorRepository indicatorRepository;

    private static final String JOB_GROUP = "indicator-jobs";
    private static final String TRIGGER_GROUP = "indicator-triggers";

    /**
     * Register (or replace) a cron schedule for an indicator.
     */
    public void schedule(String indicatorId, String cronExpression) {
        try {
            JobDetail job = JobBuilder.newJob(IndicatorScheduleJob.class)
                    .withIdentity("indicator-" + indicatorId, JOB_GROUP)
                    .usingJobData("indicatorId", indicatorId)
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + indicatorId, TRIGGER_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            if (scheduler.checkExists(job.getKey())) {
                scheduler.deleteJob(job.getKey());
            }
            scheduler.scheduleJob(job, trigger);
            log.info("Scheduled indicator {}: {}", indicatorId, cronExpression);
        } catch (Exception e) {
            log.error("Failed to schedule indicator {}: {}", indicatorId, e.getMessage());
            throw new BusinessException("调度表达式无效或调度失败: " + e.getMessage());
        }
    }

    /**
     * Remove the schedule of an indicator.
     */
    public void unschedule(String indicatorId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("indicator-" + indicatorId, JOB_GROUP));
            log.info("Unscheduled indicator {}", indicatorId);
        } catch (SchedulerException e) {
            log.error("Failed to unschedule indicator {}: {}", indicatorId, e.getMessage());
        }
    }

    /**
     * Sync the schedule of an indicator with its entity state.
     */
    public void sync(IndicatorEntity entity) {
        if (Boolean.TRUE.equals(entity.getScheduleEnabled())
                && "ENABLED".equals(entity.getStatus())
                && entity.getScheduleCron() != null
                && !entity.getScheduleCron().isBlank()) {
            schedule(entity.getId(), entity.getScheduleCron());
        } else {
            unschedule(entity.getId());
        }
    }

    /**
     * Load all scheduled indicators at startup and register their jobs.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshAllOnStartup() {
        List<IndicatorEntity> all = indicatorRepository.findAll();
        int count = 0;
        for (IndicatorEntity entity : all) {
            if (Boolean.TRUE.equals(entity.getScheduleEnabled())
                    && "ENABLED".equals(entity.getStatus())
                    && entity.getScheduleCron() != null
                    && !entity.getScheduleCron().isBlank()) {
                try {
                    schedule(entity.getId(), entity.getScheduleCron());
                    count++;
                } catch (BusinessException e) {
                    log.warn("Skip invalid schedule for indicator {}: {}", entity.getId(), e.getMessage());
                }
            }
        }
        if (count > 0) {
            log.info("Registered {} scheduled indicators", count);
        }
    }
}