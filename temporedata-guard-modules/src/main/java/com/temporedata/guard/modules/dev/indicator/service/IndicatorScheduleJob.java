package com.temporedata.guard.modules.dev.indicator.service;

import com.temporedata.guard.modules.dev.indicator.entity.IndicatorEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Quartz Job that executes an indicator by its id.
 * Used by the indicator scheduler; tenant ownership is inherited from the
 * indicator entity itself (no login context available in scheduled runs).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorScheduleJob implements Job {

    private final IndicatorService indicatorService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String indicatorId = dataMap.getString("indicatorId");
        IndicatorEntity entity = indicatorService.findEntity(indicatorId);
        if (entity == null) {
            log.warn("Scheduled indicator not found, skipping: {}", indicatorId);
            return;
        }
        if (!Boolean.TRUE.equals(entity.getScheduleEnabled()) || !"ENABLED".equals(entity.getStatus())) {
            log.info("Indicator schedule disabled, skipping: {}", indicatorId);
            return;
        }
        log.info("Quartz triggered indicator execution: {} ({})", entity.getCode(), indicatorId);
        indicatorService.executeScheduled(indicatorId);
    }
}