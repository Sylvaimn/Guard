package com.temporedata.guard.modules.common.eventbus;
import com.temporedata.guard.modules.security.security.service.AuditLogService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka 事件消费者: 任务完成/预警事件落地审计。
 * 启用条件: event.bus.type=kafka 且 Kafka 集群可用(KAFKA_SERVERS)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.audit.listener.enabled", havingValue = "true")
public class KafkaEventListener {

    private final ObjectMapper objectMapper;
    private final com.temporedata.guard.modules.security.security.service.AuditLogService auditRecordService;

    /** 任务完成事件 -> 审计留痕 */
    @KafkaListener(topics = "tdg-task", groupId = "tdg")
    public void onTask(Map<String, Object> event) {
        log.info("[KAFKA] tdg-task event: {}", safeJson(event));
        auditRecordService.record("system", "AI_TASK", "异步任务完成: " + event.get("type") + "(" + event.get("status") + ")", "KAFKA_EVENT", null, null, "SUCCESS", null, "DEFAULT");
    }

    /** 预警事件 -> 审计留痕 */
    @KafkaListener(topics = "tdg-alert", groupId = "tdg")
    public void onAlert(Map<String, Object> event) {
        log.info("[KAFKA] tdg-alert event: {}", safeJson(event));
        auditRecordService.record("system", "ALERT", "安全预警事件: " + event.get("eventType"), "KAFKA_EVENT", null, null, "SUCCESS", null, "DEFAULT");
    }

    private String safeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
