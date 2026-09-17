package com.temporedata.guard.modules.common.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.modules.dev.lineage.service.LineageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 接口血缘事件消费者: 跨层血缘登记(表 -> API 接口出口)。
 * 仅主应用启用(app.lineage.enabled=true), 拆分服务不加载, 避免依赖血缘全链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.lineage.enabled", havingValue = "true")
public class KafkaLineageListener {

    private final ObjectMapper objectMapper;
    private final LineageService lineageService;

    /** 接口血缘事件 -> 跨层血缘登记(数据服务发, 主应用登记) */
    @KafkaListener(topics = "tdg-lineage", groupId = "tdg")
    public void onApiLineage(Map<String, Object> event) {
        try {
            log.info("[KAFKA] tdg-lineage event: {}", safeJson(event));
            lineageService.collectApiLineage(
                    String.valueOf(event.get("tenantId")),
                    String.valueOf(event.get("apiId")),
                    String.valueOf(event.get("apiName")),
                    String.valueOf(event.get("sql")));
        } catch (Exception e) {
            log.warn("api lineage collect failed: {}", e.getMessage());
        }
    }

    private String safeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }
}
