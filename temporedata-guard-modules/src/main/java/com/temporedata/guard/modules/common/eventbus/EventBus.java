package com.temporedata.guard.modules.common.eventbus;

import java.util.Map;

/**
 * 分布式事件总线抽象: 任务完成/审计/预警等事件异步发布。
 * event.bus.type=direct(默认, 同步直连) | kafka(独立集群, 跨服务解耦)
 */
public interface EventBus {

    /** 发布事件 */
    void publish(String topic, Object payload);

    /** 发布带元数据事件 */
    void publish(String topic, String eventType, Map<String, Object> payload);
}
