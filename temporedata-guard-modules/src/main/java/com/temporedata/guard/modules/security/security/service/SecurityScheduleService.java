/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.PostConstruct
 *  org.quartz.CronScheduleBuilder
 *  org.quartz.CronTrigger
 *  org.quartz.JobBuilder
 *  org.quartz.JobDetail
 *  org.quartz.ScheduleBuilder
 *  org.quartz.Scheduler
 *  org.quartz.Trigger
 *  org.quartz.TriggerBuilder
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.security.security.service;


import javax.annotation.PostConstruct;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityScheduleService {
    private static final Logger log = LoggerFactory.getLogger(SecurityScheduleService.class);
    private final Scheduler scheduler;
    private static final String JOB_GROUP = "security-jobs";
    private static final String TRIGGER_GROUP = "security-triggers";

    @PostConstruct
    public void register() {
        this.schedule("EXPIRY", "0 */10 * * * ?");
        this.schedule("ANOMALY", "0 */5 * * * ?");
    }

    private void schedule(String jobType, String cron) {
        try {
            JobDetail job = JobBuilder.newJob(SecurityJob.class).withIdentity("security-" + jobType, JOB_GROUP).usingJobData("jobType", jobType).storeDurably().build();
            CronTrigger trigger = (CronTrigger)TriggerBuilder.newTrigger().withIdentity("security-trigger-" + jobType, TRIGGER_GROUP).withSchedule((ScheduleBuilder)CronScheduleBuilder.cronSchedule((String)cron).withMisfireHandlingInstructionDoNothing()).build();
            if (this.scheduler.checkExists(job.getKey())) {
                this.scheduler.deleteJob(job.getKey());
            }
            this.scheduler.scheduleJob(job, (Trigger)trigger);
            log.info("Scheduled security job {}: {}", (Object)jobType, (Object)cron);
        }
        catch (Exception e) {
            log.error("Failed to schedule security job {}: {}", (Object)jobType, (Object)e.getMessage());
        }
    }

    public SecurityScheduleService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}

