/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.agent.aianalysis.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.security.security.DensityGuard;
import com.temporedata.guard.security.LoginUser;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DynamicAuthzService {
    private static final Logger log = LoggerFactory.getLogger(DynamicAuthzService.class);
    private static final Map<String, String> TOOL_MIN_DENSITY = Map.of("track_query", "CONFIDENTIAL", "graph_expand", "CONFIDENTIAL", "entity_search", "INTERNAL", "watch_status", "INTERNAL");

    public void check(LoginUser user, String toolName) {
        if (user == null || !user.isEnabled()) {
            throw new BusinessException("\u8d26\u53f7\u4e0d\u53ef\u7528\uff0c\u65e0\u6cd5\u6267\u884c\u5de5\u5177");
        }
        if (user.getRoleCodes() != null && user.getRoleCodes().contains("ADMIN")) {
            return;
        }
        String required = TOOL_MIN_DENSITY.getOrDefault(toolName, "INTERNAL");
        if (!DensityGuard.canAccess(user.getDensity(), required)) {
            log.warn("[AUTHZ] {} blocked: user density {} < required {} for tool {}", new Object[]{user.getUsername(), user.getDensity(), required, toolName});
            throw new BusinessException("\u5bc6\u7ea7\u4e0d\u8db3\uff0c\u65e0\u6743\u8c03\u7528\u5de5\u5177: " + toolName);
        }
    }
}

