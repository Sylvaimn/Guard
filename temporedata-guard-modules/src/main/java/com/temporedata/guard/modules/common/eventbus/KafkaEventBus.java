package com.temporedata.guard.modules.common.eventbus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka 事件总线: event.bus.type=kafka 时启用, 事件异步发布到 Kafka 集群, 实现跨服务解耦。
 * 拓扑: tdg-task(任务) / tdg-audit(审计) / tdg-alert(预警) / tdg-notify(消息)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.bus.type", havingValue = "kafka")
public class KafkaEventBus implements EventBus {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, Object payload) {
        kafkaTemplate.send(topic, payload);
    }

    @Override
    public void publish(String topic, String eventType, Map<String, Object> payload) {
        Map<String, Object> event = new java.util.HashMap<>(payload);
        event.put("eventType", eventType);
        event.put("ts", System.currentTimeMillis());
        kafkaTemplate.send(topic, event);
    }
}
