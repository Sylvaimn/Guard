/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.quartz.Job
 *  org.quartz.JobDataMap
 *  org.quartz.JobExecutionContext
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.temporedata.guard.modules.security.security.service;

import com.temporedata.guard.modules.common.async.TaskExecutorService;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityJob
implements Job {
    private static final Logger log = LoggerFactory.getLogger(SecurityJob.class);
    private final SecurityService securityService;
    private final TaskExecutorService taskExecutorService;

    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        String jobType = data.getString("jobType");
        if (jobType == null || jobType.isEmpty()) {
            log.warn("SecurityJob triggered without jobType");
            return;
        }
        boolean accepted = this.taskExecutorService.submit("security:" + jobType, () -> {
            try {
                int n;
                if ("EXPIRY".equals(jobType)) {
                    int n2 = this.securityService.recycleExpiredApprovals();
                    if (n2 > 0) {
                        log.info("[SECURITY] recycled {} expired approvals", (Object)n2);
                    }
                } else if ("ANOMALY".equals(jobType) && (n = this.securityService.scanApiAnomalies()) > 0) {
                    log.info("[SECURITY] pushed {} anomaly alerts", (Object)n);
                }
            }
            catch (Exception e) {
                log.error("SecurityJob {} failed: {}", (Object)jobType, (Object)e.getMessage());
            }
        });
        if (!accepted) {
            log.warn("SecurityJob {} already running, skipped", (Object)jobType);
        }
    }

    public SecurityJob(SecurityService securityService, TaskExecutorService taskExecutorService) {
        this.securityService = securityService;
        this.taskExecutorService = taskExecutorService;
    }
}

