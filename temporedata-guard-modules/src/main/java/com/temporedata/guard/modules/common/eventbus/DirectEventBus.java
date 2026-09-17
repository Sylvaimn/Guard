package com.temporedata.guard.modules.common.eventbus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 同步直连事件总线(默认): 单机/未接入消息队列时的降级实现, 事件直接透传。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "event.bus.type", havingValue = "direct", matchIfMissing = true)
public class DirectEventBus implements EventBus {

    @Override
    public void publish(String topic, Object payload) {
        log.debug("[EVENT:direct] topic={} payload={}", topic, payload);
    }

    @Override
    public void publish(String topic, String eventType, Map<String, Object> payload) {
        log.debug("[EVENT:direct] topic={} type={} payload={}", topic, eventType, payload);
    }
}
