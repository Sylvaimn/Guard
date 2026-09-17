/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.agent.aianalysis.service;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PromptGuard {
    private static final Logger log = LoggerFactory.getLogger(PromptGuard.class);
    private static final int MAX_INPUT_LEN = 500;
    private static final Pattern[] INJECTION_PATTERNS = new Pattern[]{Pattern.compile("(?i)\u5ffd\u7565.*(\u6307\u4ee4|\u63d0\u793a|\u4ee5\u4e0a|\u4e4b\u524d).*"), Pattern.compile("(?i)\u65e0\u89c6.*(\u6307\u4ee4|\u63d0\u793a|\u4ee5\u4e0a|\u4e4b\u524d).*"), Pattern.compile("(?i)ignore (all |the )?(previous|above|prior).*instructions"), Pattern.compile("(?i)system\\s*(prompt|role)"), Pattern.compile("(?i)role\\s*[:=]\\s*system"), Pattern.compile("(?i)\u4f60(\u73b0\u5728|\u5c06).*(\u626e\u6f14|\u4f5c\u4e3a).*\u7cfb\u7edf"), Pattern.compile("(?i)\u8f93\u51fa.*(\u539f\u59cb|\u5185\u90e8).*(\u6307\u4ee4|\u63d0\u793a)"), Pattern.compile("(?i)disregard.*instructions")};

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        String text = input.trim();
        if (text.isEmpty()) {
            return null;
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (!p.matcher(text).find()) continue;
            log.warn("[PROMPT-GUARD] injection pattern blocked: {}", (Object)p.pattern());
            return null;
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}

