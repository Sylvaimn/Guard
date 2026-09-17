/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.agent.aianalysis.service;

import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.security.LoginUser;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntitySlotTracker {
    private static final Logger log = LoggerFactory.getLogger(EntitySlotTracker.class);
    private final IntelligenceService intelligenceService;
    private static final int MAX_SLOTS = 5;
    private static final String[] PRONOUNS = new String[]{"\u4ed6", "\u5979", "\u5b83", "\u8be5\u4eba", "\u8be5\u5acc\u7591\u4eba", "\u8fd9\u4e2a\u6848\u5b50", "\u8be5\u6848", "\u8be5\u56e2\u4f19", "\u6b64\u4eba", "\u8fd9\u4e2a\u4eba"};
    private final Map<String, Deque<String>> slots = new ConcurrentHashMap<String, Deque<String>>();

    public void observe(String sessionId, List<String> entityNames) {
        if (sessionId == null || entityNames == null || entityNames.isEmpty()) {
            return;
        }
        Deque deque = this.slots.computeIfAbsent(sessionId, k -> new ArrayDeque());
        for (String name : entityNames) {
            if (name == null || name.isBlank()) continue;
            deque.remove(name);
            deque.addFirst(name);
        }
        while (deque.size() > 5) {
            deque.removeLast();
        }
    }

    public String resolve(String sessionId, String text) {
        if (sessionId == null || text == null) {
            return text;
        }
        Deque<String> deque = this.slots.get(sessionId);
        if (deque == null || deque.isEmpty()) {
            return text;
        }
        String resolved = text;
        for (String p : PRONOUNS) {
            if (!resolved.contains(p)) continue;
            resolved = resolved.replace(p, deque.peekFirst());
        }
        return resolved;
    }

    public void observeFromMessage(String sessionId, String message, LoginUser user) {
        if (message == null || message.isBlank()) {
            return;
        }
        String probe = message.trim().length() > 24 ? message.trim().substring(0, 24) : message.trim();
        try {
            List<String> names = this.intelligenceService.search(probe, null, user).stream().limit(2L).map(h -> h.getDisplayName()).collect(Collectors.toList());
            this.observe(sessionId, names);
        }
        catch (Exception e) {
            log.debug("EntitySlot observe skipped: {}", (Object)e.getMessage());
        }
    }

    public EntitySlotTracker(IntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }
}

