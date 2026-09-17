package com.temporedata.guard.modules.dev.quality.service;

import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 质量规则定时调度：scheduleCron 非空的 ACTIVE 规则注册到 Quartz 集群任务，
 * 定时执行质量检查（集群模式下跨节点单触发）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityScheduleService {

    private final Scheduler scheduler;
    private final QualityRuleRepository ruleRepository;

    private static final String JOB_GROUP = "quality-jobs";
    private static final String TRIGGER_GROUP = "quality-triggers";

    public void scheduleRule(String ruleId, String cron) {
        try {
            JobDetail job = JobBuilder.newJob(QualityJob.class)
                    .withIdentity("quality-" + ruleId, JOB_GROUP)
                    .usingJobData("ruleId", ruleId)
                    .storeDurably()
                    .build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("qtrigger-" + ruleId, TRIGGER_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            if (scheduler.checkExists(job.getKey())) {
                scheduler.deleteJob(job.getKey());
            }
            scheduler.scheduleJob(job, trigger);
            log.info("Scheduled quality rule {}: {}", ruleId, cron);
        } catch (Exception e) {
            log.error("Failed to schedule quality rule {}: {}", ruleId, e.getMessage());
        }
    }

    public void removeSchedule(String ruleId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("quality-" + ruleId, JOB_GROUP));
        } catch (SchedulerException e) {
            log.error("Failed to remove quality schedule {}: {}", ruleId, e.getMessage());
        }
    }

    /** 启动时注册全部已配置的规则调度 */
    public void registerAll() {
        List<QualityRuleEntity> rules = ruleRepository.findAll();
        int scheduled = 0;
        for (QualityRuleEntity rule : rules) {
            if ("ACTIVE".equals(rule.getStatus()) && rule.getScheduleCron() != null
                    && !rule.getScheduleCron().isBlank()) {
                scheduleRule(rule.getId(), rule.getScheduleCron());
                scheduled++;
            }
        }
        log.info("Quality scheduler: {} rules registered", scheduled);
    }
}
