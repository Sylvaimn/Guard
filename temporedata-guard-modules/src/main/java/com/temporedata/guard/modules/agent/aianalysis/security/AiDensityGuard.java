/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.temporedata.guard.modules.agent.aianalysis.security;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.security.security.DensityGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AiDensityGuard {
    private static final Logger log = LoggerFactory.getLogger(AiDensityGuard.class);
    private static final String EXTERNAL_MODEL_MAX_DENSITY = "INTERNAL";

    private AiDensityGuard() {
    }

    public static boolean canSendExternal(String dataDensity, boolean localModel) {
        if (localModel) {
            return true;
        }
        return DensityGuard.rank(dataDensity) <= DensityGuard.rank(EXTERNAL_MODEL_MAX_DENSITY);
    }

    public static void requireSendAllowed(String userDensity, boolean localModel, String scene) {
        if (!AiDensityGuard.canSendExternal(userDensity, localModel)) {
            log.warn("[AI-DENSITY] blocked external model call: scene={} density={}", (Object)scene, (Object)userDensity);
            throw new BusinessException("\u6d89\u5bc6\u8bf7\u6c42\u4e0d\u5141\u8bb8\u4f7f\u7528\u5916\u90e8\u6a21\u578b\uff0c\u8bf7\u914d\u7f6e\u672c\u5730\u79c1\u6709\u5316\u6a21\u578b\u6216\u964d\u4f4e\u5bc6\u7ea7");
        }
    }
}

