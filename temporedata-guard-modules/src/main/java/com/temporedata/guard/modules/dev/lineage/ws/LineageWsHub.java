package com.temporedata.guard.modules.dev.lineage.ws;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Lineage change event hub: keeps WebSocket sessions per tenant,
 * publishes lineage.updated events after collection/rebuild.
 * Equivalent of the design's real-time change notification channel.
 */
@Slf4j
@Component
public class LineageWsHub {

    private static final Map<String, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    public static void register(String tenantId, WebSocketSession session) {
        SESSIONS.computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("Lineage WS connected: tenant={}, sessions={}", tenantId, SESSIONS.get(tenantId).size());
    }

    public static void unregister(String tenantId, WebSocketSession session) {
        Set<WebSocketSession> sessions = SESSIONS.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) SESSIONS.remove(tenantId);
        }
    }

    public static void publish(String tenantId, String type, String source, String taskId) {
        Set<WebSocketSession> sessions = SESSIONS.get(tenantId);
        if (sessions == null || sessions.isEmpty()) return;
        Map<String, Object> payload = Map.of(
                "type", type,
                "source", source == null ? "" : source,
                "taskId", taskId == null ? "" : taskId,
                "timestamp", System.currentTimeMillis());
        TextMessage message = new TextMessage(JSONUtil.toJsonStr(payload));
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                } else {
                    sessions.remove(session);
                }
            } catch (Exception e) {
                log.warn("Lineage WS publish failed: {}", e.getMessage());
                sessions.remove(session);
            }
        }
    }
}