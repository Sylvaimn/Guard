package com.temporedata.guard.modules.dev.sync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

/**
 * Quartz job for scheduled INCREMENTAL (CDC poll) sync tasks.
 */
@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class SyncJob implements Job {

    private final SyncService syncService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        String taskId = data.getString("taskId");
        if (taskId == null || taskId.isEmpty()) {
            log.warn("SyncJob triggered without taskId");
            return;
        }
        try {
            syncService.executeAsync(taskId);
        } catch (Exception e) {
            log.error("Scheduled sync task {} failed: {}", taskId, e.getMessage());
        }
    }
}
