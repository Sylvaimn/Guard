package com.temporedata.guard.modules.dev.quality.service;
import com.temporedata.guard.modules.common.async.TaskExecutorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

/**
 * Quartz 定时质量检查任务：调度中心统一触发(集群单触发), 单飞防并发。
 */
@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class QualityJob implements Job {

    private final QualityService qualityService;
    private final com.temporedata.guard.modules.common.async.TaskExecutorService taskExecutorService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        String ruleId = data.getString("ruleId");
        if (ruleId == null || ruleId.isEmpty()) {
            log.warn("QualityJob triggered without ruleId");
            return;
        }
        boolean accepted = taskExecutorService.submit("quality:" + ruleId, () -> {
            try {
                qualityService.execute(ruleId);
            } catch (Exception e) {
                log.error("Scheduled quality check {} failed: {}", ruleId, e.getMessage());
            }
        });
        if (!accepted) {
            log.warn("Quality check {} already running, skipped", ruleId);
        }
    }
}
