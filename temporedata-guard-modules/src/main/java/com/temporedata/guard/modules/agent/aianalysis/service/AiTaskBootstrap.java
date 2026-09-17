/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.agent.aianalysis.service;



import com.temporedata.guard.security.LoginUser;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value=30)
public class AiTaskBootstrap
implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AiTaskBootstrap.class);
    private final AiTaskService aiTaskService;
    private final ReportComposeService reportComposeService;

    public void run(String ... args) {
        this.aiTaskService.registerHandler("REPORT", payload -> {
            String entityId = String.valueOf(payload.getOrDefault("entityId", ""));
            String name = String.valueOf(payload.getOrDefault("name", entityId));
            String tenantId = String.valueOf(payload.getOrDefault("tenantId", "DEFAULT"));
            LoginUser sysUser = new LoginUser("system", "system", null, tenantId, "SECRET", Set.of("ADMIN"), true);
            try {
                Thread.sleep(500L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String reportId = this.reportComposeService.compose(entityId, tenantId, sysUser, null);
            log.info("[AI-TASK] REPORT composed for {} -> {}", (Object)name, (Object)reportId);
            return "report:" + reportId;
        });
        this.aiTaskService.registerHandler("ANALYSIS", payload -> {
            String target = String.valueOf(payload.getOrDefault("target", ""));
            try {
                Thread.sleep(1500L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "analysis:" + Integer.toHexString(target.hashCode());
        });
        log.info("AiTask handlers registered: REPORT, ANALYSIS");
    }

    public AiTaskBootstrap(AiTaskService aiTaskService, ReportComposeService reportComposeService) {
        this.aiTaskService = aiTaskService;
        this.reportComposeService = reportComposeService;
    }
}

