/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.security.service;
import com.temporedata.guard.modules.security.apilog.entity.ApiLogEntity;
import com.temporedata.guard.modules.security.apilog.repository.ApiLogRepository;
import com.temporedata.guard.modules.security.message.service.MessageService;
import com.temporedata.guard.modules.security.permapproval.entity.PermApprovalEntity;
import com.temporedata.guard.modules.security.permapproval.repository.PermApprovalRepository;



import com.temporedata.guard.modules.common.eventbus.EventBus;



import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);
    private final PermApprovalRepository approvalRepository;
    private final ApiLogRepository apiLogRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final EventBus eventBus;

    @Transactional
    public int recycleExpiredApprovals() {
        List<PermApprovalEntity> approved = this.approvalRepository.findByStatusOrderByCreateTimeDesc("APPROVED");
        int recycled = 0;
        for (PermApprovalEntity a : approved) {
            if (a.getExpireTime() == null || !a.getExpireTime().isBefore(LocalDateTime.now())) continue;
            a.setStatus("EXPIRED");
            a.setUpdateTime(LocalDateTime.now());
            this.approvalRepository.save(a);
            try {
                this.messageService.send(a.getApplicantId(), "APPROVAL", "\u4e34\u65f6\u6743\u9650\u5df2\u5230\u671f\u56de\u6536", "\u60a8\u7533\u8bf7\u7684\u300c" + a.getResourceName() + "\u300d\u9650\u65f6\u6743\u9650\u5df2\u4e8e " + a.getExpireTime() + " \u5230\u671f\uff0c\u7cfb\u7edf\u5df2\u81ea\u52a8\u56de\u6536\u3002\u5982\u9700\u7ee7\u7eed\u4f7f\u7528\u8bf7\u91cd\u65b0\u7533\u8bf7\u3002", "APPROVAL", a.getId(), a.getTenantId());
            }
            catch (Exception e) {
                log.warn("notify expiry failed: {}", (Object)e.getMessage());
            }
            this.eventBus.publish("tdg-alert", "PERM_EXPIRED", Map.of("approvalId", a.getId(), "resource", a.getResourceName(), "applicant", a.getApplicantId(), "tenantId", a.getTenantId()));
            ++recycled;
        }
        return recycled;
    }

    public int scanApiAnomalies() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(10L);
        List<ApiLogEntity> recent = this.apiLogRepository.findByCreateTimeAfter(since);
        long anomalies = recent.stream().filter(l -> l.getResponseCode() != null && l.getResponseCode() != 200 || l.getCostTime() != null && l.getCostTime() > 3000L).count();
        if (anomalies < 10L) {
            return 0;
        }
        Optional<UserEntity> admin = this.userRepository.findByUsername("admin");
        if (admin.isEmpty()) {
            return 0;
        }
        this.messageService.send(admin.get().getId(), "ALERT", "\u63a5\u53e3\u8c03\u7528\u5f02\u5e38\u544a\u8b66", "\u8fd1 10 \u5206\u949f\u68c0\u6d4b\u5230 " + anomalies + " \u6b21\u5f02\u5e38\u8c03\u7528\uff08\u5931\u8d25/\u8d85\u65f6>3s\uff09\uff0c\u8bf7\u5230\u300c\u8b66\u52a1\u6570\u636e\u63a5\u53e3\u2192\u63a5\u53e3\u8c03\u7528\u5ba1\u8ba1\u300d\u6838\u67e5\u3002", "ALERT", "api-anomaly", admin.get().getTenantId());
        return 1;
    }

    public SecurityService(PermApprovalRepository approvalRepository, ApiLogRepository apiLogRepository, UserRepository userRepository, MessageService messageService, EventBus eventBus) {
        this.approvalRepository = approvalRepository;
        this.apiLogRepository = apiLogRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.eventBus = eventBus;
    }
}

