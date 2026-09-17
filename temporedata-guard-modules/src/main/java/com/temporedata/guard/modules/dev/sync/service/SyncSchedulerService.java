package com.temporedata.guard.modules.dev.sync.service;

import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 增量同步任务轮询调度：INCREMENTAL + scheduleCron 的任务注册到 Quartz 定时执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncSchedulerService {

    private final Scheduler scheduler;
    private final SyncTaskRepository taskRepository;

    private static final String JOB_GROUP = "sync-jobs";
    private static final String TRIGGER_GROUP = "sync-triggers";

    public void scheduleTask(String taskId, String cron) {
        try {
            JobDetail job = JobBuilder.newJob(SyncJob.class)
                    .withIdentity("sync-" + taskId, JOB_GROUP)
                    .usingJobData("taskId", taskId)
                    .storeDurably()
                    .build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + taskId, TRIGGER_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            if (scheduler.checkExists(job.getKey())) {
                scheduler.deleteJob(job.getKey());
            }
            scheduler.scheduleJob(job, trigger);
            log.info("Scheduled sync task {}: {}", taskId, cron);
        } catch (Exception e) {
            log.error("Failed to schedule sync task {}: {}", taskId, e.getMessage());
        }
    }

    public void removeSchedule(String taskId) {
        try {
            JobKey jobKey = JobKey.jobKey("sync-" + taskId, JOB_GROUP);
            TriggerKey triggerKey = TriggerKey.triggerKey("trigger-" + taskId, TRIGGER_GROUP);
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("Failed to remove sync schedule {}: {}", taskId, e.getMessage());
        }
    }

    public void registerAll() {
        List<SyncTaskEntity> tasks = taskRepository.findAll();
        for (SyncTaskEntity t : tasks) {
            if ("INCREMENTAL".equals(t.getSyncMode())
                    && t.getScheduleCron() != null && !t.getScheduleCron().isEmpty()) {
                scheduleTask(t.getId(), t.getScheduleCron());
            }
        }
        log.info("Sync scheduler registered {} incremental tasks", tasks.stream()
                .filter(t -> "INCREMENTAL".equals(t.getSyncMode()) && t.getScheduleCron() != null).count());
    }
}
