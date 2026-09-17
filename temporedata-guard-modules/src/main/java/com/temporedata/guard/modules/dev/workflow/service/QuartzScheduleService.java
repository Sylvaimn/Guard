package com.temporedata.guard.modules.dev.workflow.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Quartz-based schedule management service.
 * Replaces the simple TaskScheduler with persisted Quartz jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzScheduleService {

    private final Scheduler scheduler;
    private final WorkflowRepository workflowRepository;

    private static final String JOB_GROUP = "workflow-jobs";
    private static final String TRIGGER_GROUP = "workflow-triggers";

    /**
     * Schedule a workflow with a cron expression.
     */
    public void scheduleWorkflow(String workflowId, String cronExpression) {
        try {
            JobDetail job = JobBuilder.newJob(WorkflowJob.class)
                    .withIdentity("workflow-" + workflowId, JOB_GROUP)
                    .usingJobData("workflowId", workflowId)
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + workflowId, TRIGGER_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            // Remove existing job if present
            if (scheduler.checkExists(job.getKey())) {
                scheduler.deleteJob(job.getKey());
            }

            scheduler.scheduleJob(job, trigger);
            log.info("Scheduled workflow {}: {}", workflowId, cronExpression);
        } catch (Exception e) {
            log.error("Failed to schedule workflow {}: {}", workflowId, e.getMessage());
            throw new BusinessException("Failed to schedule workflow: " + e.getMessage());
        }
    }

    /**
     * Pause a workflow schedule without removing it.
     */
    public void pauseSchedule(String workflowId) {
        try {
            scheduler.pauseJob(JobKey.jobKey("workflow-" + workflowId, JOB_GROUP));
            log.info("Paused schedule for workflow: {}", workflowId);
        } catch (SchedulerException e) {
            log.error("Failed to pause schedule for workflow {}: {}", workflowId, e.getMessage());
            throw new BusinessException("Failed to pause schedule: " + e.getMessage());
        }
    }

    /**
     * Resume a paused workflow schedule.
     */
    public void resumeSchedule(String workflowId) {
        try {
            scheduler.resumeJob(JobKey.jobKey("workflow-" + workflowId, JOB_GROUP));
            log.info("Resumed schedule for workflow: {}", workflowId);
        } catch (SchedulerException e) {
            log.error("Failed to resume schedule for workflow {}: {}", workflowId, e.getMessage());
            throw new BusinessException("Failed to resume schedule: " + e.getMessage());
        }
    }

    /**
     * Remove a workflow schedule completely.
     */
    public void removeSchedule(String workflowId) {
        try {
            JobKey jobKey = JobKey.jobKey("workflow-" + workflowId, JOB_GROUP);
            TriggerKey triggerKey = TriggerKey.triggerKey("trigger-" + workflowId, TRIGGER_GROUP);

            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);

            log.info("Removed schedule for workflow: {}", workflowId);
        } catch (SchedulerException e) {
            log.error("Failed to remove schedule for workflow {}: {}", workflowId, e.getMessage());
        }
    }

    /**
     * Check if a workflow has an active schedule.
     */
    public boolean isScheduled(String workflowId) {
        try {
            return scheduler.checkExists(JobKey.jobKey("workflow-" + workflowId, JOB_GROUP));
        } catch (SchedulerException e) {
            return false;
        }
    }

    /**
     * Trigger a workflow execution immediately (one-time, not rescheduled).
     */
    public void triggerNow(String workflowId) {
        try {
            scheduler.triggerJob(JobKey.jobKey("workflow-" + workflowId, JOB_GROUP));
            log.info("Triggered immediate execution for workflow: {}", workflowId);
        } catch (SchedulerException e) {
            log.error("Failed to trigger workflow {}: {}", workflowId, e.getMessage());
            throw new BusinessException("Failed to trigger workflow: " + e.getMessage());
        }
    }

    /**
     * Re-register all active schedules on startup.
     */
    public void registerAllSchedules() {
        List<WorkflowEntity> workflows = workflowRepository.findByStatus("ONLINE");
        for (WorkflowEntity wf : workflows) {
            if (wf.getCronExpression() != null && !wf.getCronExpression().isEmpty()) {
                try {
                    scheduleWorkflow(wf.getId(), wf.getCronExpression());
                    log.info("Re-registered schedule for workflow: {}", wf.getName());
                } catch (Exception e) {
                    log.error("Failed to re-register schedule for workflow {}: {}", wf.getId(), e.getMessage());
                }
            }
        }
    }
}