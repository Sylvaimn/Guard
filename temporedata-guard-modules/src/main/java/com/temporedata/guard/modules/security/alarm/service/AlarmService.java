/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.alarm.AlarmConfigReq
 *  com.temporedata.guard.api.alarm.AlarmConfigRes
 *  com.temporedata.guard.api.alarm.AlarmRecordRes
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.context.event.EventListener
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.alarm.service;
import com.temporedata.guard.modules.security.alarm.entity.AlarmConfigEntity;
import com.temporedata.guard.modules.security.alarm.repository.AlarmConfigRepository;
import com.temporedata.guard.modules.security.alarm.entity.AlarmRecordEntity;
import com.temporedata.guard.modules.security.alarm.repository.AlarmRecordRepository;
import com.temporedata.guard.modules.security.alarm.event.QualityCheckFailedEvent;
import com.temporedata.guard.modules.security.alarm.event.WorkflowFailedEvent;

import com.temporedata.guard.api.alarm.AlarmConfigReq;
import com.temporedata.guard.api.alarm.AlarmConfigRes;
import com.temporedata.guard.api.alarm.AlarmRecordRes;
import com.temporedata.guard.api.base.exceptions.BusinessException;






import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmService {
    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);
    private final AlarmConfigRepository alarmConfigRepository;
    private final AlarmRecordRepository alarmRecordRepository;

    public List<AlarmConfigRes> list(String tenantId) {
        return this.alarmConfigRepository.findByTenantId(tenantId).stream().map(this::toRes).collect(Collectors.toList());
    }

    public AlarmConfigRes detail(String id) {
        return this.toRes((AlarmConfigEntity)this.alarmConfigRepository.findById(id).orElseThrow(() -> new BusinessException("\u544a\u8b66\u914d\u7f6e\u4e0d\u5b58\u5728")));
    }

    @Transactional
    public AlarmConfigRes create(AlarmConfigReq req, String tenantId) {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setEventType(req.getEventType());
        entity.setChannels(req.getChannels());
        entity.setEmail(req.getEmail());
        entity.setWebhookUrl(req.getWebhookUrl());
        entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((AlarmConfigEntity)this.alarmConfigRepository.save(entity));
    }

    @Transactional
    public AlarmConfigRes update(String id, AlarmConfigReq req) {
        AlarmConfigEntity entity = (AlarmConfigEntity)this.alarmConfigRepository.findById(id).orElseThrow(() -> new BusinessException("\u544a\u8b66\u914d\u7f6e\u4e0d\u5b58\u5728"));
        entity.setName(req.getName());
        entity.setEventType(req.getEventType());
        entity.setChannels(req.getChannels());
        entity.setEmail(req.getEmail());
        entity.setWebhookUrl(req.getWebhookUrl());
        entity.setEnabled(req.getEnabled());
        return this.toRes((AlarmConfigEntity)this.alarmConfigRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        this.alarmConfigRepository.deleteById(id);
    }

    public String sendAlarm(String configId, String message) {
        AlarmConfigEntity config = (AlarmConfigEntity)this.alarmConfigRepository.findById(configId).orElseThrow(() -> new BusinessException("\u544a\u8b66\u914d\u7f6e\u4e0d\u5b58\u5728"));
        if (!config.getEnabled().booleanValue()) {
            return "\u544a\u8b66\u914d\u7f6e\u5df2\u7981\u7528\uff0c\u8df3\u8fc7\u53d1\u9001";
        }
        String status = "SENT";
        try {
            String channels = config.getChannels();
            if (channels != null) {
                if (channels.contains("EMAIL") && config.getEmail() != null) {
                    this.sendEmail(config.getEmail(), message);
                }
                if (channels.contains("WEBHOOK") && config.getWebhookUrl() != null) {
                    this.sendWebhook(config.getWebhookUrl(), message);
                }
                if (channels.contains("SMS")) {
                    this.sendSms(message);
                }
            }
        }
        catch (Exception e) {
            log.error("Failed to send alarm for config {}", (Object)configId, (Object)e);
            status = "FAILED";
        }
        AlarmRecordEntity record = new AlarmRecordEntity();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setConfigId(configId);
        record.setEventType(config.getEventType());
        record.setMessage(message);
        record.setStatus(status);
        record.setSendTime(LocalDateTime.now());
        record.setTenantId(config.getTenantId());
        this.alarmRecordRepository.save(record);
        return status;
    }

    public String testAlarm(String configId) {
        return this.sendAlarm(configId, "\u8fd9\u662f\u4e00\u6761\u6d4b\u8bd5\u544a\u8b66\u6d88\u606f");
    }

    public List<AlarmRecordRes> listRecords(String configId) {
        return this.alarmRecordRepository.findByConfigIdOrderBySendTimeDesc(configId).stream().map(this::toRecordRes).collect(Collectors.toList());
    }

    @EventListener
    public void onWorkflowFailed(WorkflowFailedEvent event) {
        log.info("Received WorkflowFailedEvent: workflowId={}, tenantId={}", (Object)event.getWorkflowId(), (Object)event.getTenantId());
        List<AlarmConfigEntity> configs = this.alarmConfigRepository.findByTenantId(event.getTenantId());
        for (AlarmConfigEntity config : configs) {
            if (!config.getEnabled().booleanValue() || !"WORKFLOW_FAILED".equals(config.getEventType())) continue;
            String message = String.format("\u5de5\u4f5c\u6d41 [%s] \u6267\u884c\u5931\u8d25: %s", event.getWorkflowName(), event.getErrorMessage());
            this.sendAlarm(config.getId(), message);
        }
    }

    @EventListener
    public void onQualityCheckFailed(QualityCheckFailedEvent event) {
        log.info("Received QualityCheckFailedEvent: ruleId={}, tenantId={}", (Object)event.getRuleId(), (Object)event.getTenantId());
        List<AlarmConfigEntity> configs = this.alarmConfigRepository.findByTenantId(event.getTenantId());
        for (AlarmConfigEntity config : configs) {
            if (!config.getEnabled().booleanValue() || !"QUALITY_FAILED".equals(config.getEventType())) continue;
            String message = String.format("\u8d28\u91cf\u89c4\u5219 [%s] \u68c0\u67e5\u5931\u8d25: %s", event.getRuleName(), event.getErrorMessage());
            this.sendAlarm(config.getId(), message);
        }
    }

    private void sendEmail(String email, String message) {
        log.info("Sending email to {}: {}", (Object)email, (Object)message);
    }

    private void sendWebhook(String webhookUrl, String message) {
        log.info("Sending webhook to {}: {}", (Object)webhookUrl, (Object)message);
    }

    private void sendSms(String message) {
        log.info("Sending SMS: {}", (Object)message);
    }

    private AlarmConfigRes toRes(AlarmConfigEntity e) {
        AlarmConfigRes res = new AlarmConfigRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setEventType(e.getEventType());
        res.setChannels(e.getChannels());
        res.setEnabled(e.getEnabled());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private AlarmRecordRes toRecordRes(AlarmRecordEntity e) {
        AlarmRecordRes res = new AlarmRecordRes();
        res.setId(e.getId());
        res.setEventType(e.getEventType());
        res.setMessage(e.getMessage());
        res.setStatus(e.getStatus());
        res.setSendTime(e.getSendTime() != null ? e.getSendTime().toString() : null);
        return res;
    }

    public AlarmService(AlarmConfigRepository alarmConfigRepository, AlarmRecordRepository alarmRecordRepository) {
        this.alarmConfigRepository = alarmConfigRepository;
        this.alarmRecordRepository = alarmRecordRepository;
    }
}

