/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.hutool.json.JSONUtil
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 *  org.springframework.web.socket.TextMessage
 *  org.springframework.web.socket.WebSocketMessage
 *  org.springframework.web.socket.WebSocketSession
 */
package com.temporedata.guard.modules.agent.aianalysis.ws;

import cn.hutool.json.JSONUtil;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AiTaskWsHub {
    private static final Logger log = LoggerFactory.getLogger(AiTaskWsHub.class);
    private static final Map<String, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<String, Set<WebSocketSession>>();
    private static final Map<String, String> SESSION_USER = new ConcurrentHashMap<String, String>();

    public static void register(String userId, String tenantId, WebSocketSession session) {
        String sid = session.getId();
        SESSIONS.computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet()).add(session);
        SESSION_USER.put(sid, userId);
    }

    public static void unregister(String tenantId, WebSocketSession session) {
        Set<WebSocketSession> sessions = SESSIONS.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                SESSIONS.remove(tenantId);
            }
        }
        SESSION_USER.remove(session.getId());
    }

    public static void publish(String userId, String tenantId, String taskId, String status, String resultRef) {
        Set<WebSocketSession> sessions = SESSIONS.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage((CharSequence)JSONUtil.toJsonStr(Map.of("type", "ai.task.done", "taskId", taskId == null ? "" : taskId, "status", status == null ? "" : status, "resultRef", resultRef == null ? "" : resultRef, "timestamp", System.currentTimeMillis())));
        for (WebSocketSession session : sessions) {
            String owner = SESSION_USER.get(session.getId());
            if (userId != null && !userId.isBlank() && owner != null && !userId.equals(owner)) continue;
            try {
                if (session.isOpen()) {
                    session.sendMessage((WebSocketMessage)message);
                    continue;
                }
                sessions.remove(session);
            }
            catch (Exception e) {
                log.warn("AiTask WS publish failed: {}", (Object)e.getMessage());
                sessions.remove(session);
            }
        }
    }
}

