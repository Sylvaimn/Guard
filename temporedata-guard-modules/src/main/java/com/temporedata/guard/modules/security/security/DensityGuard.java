/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 */
package com.temporedata.guard.modules.security.security;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import java.util.List;

public final class DensityGuard {
    private static final List<String> DENSITY_ORDER = List.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "SECRET");

    private DensityGuard() {
    }

    public static int rank(String density) {
        int idx = DENSITY_ORDER.indexOf(density == null ? "" : density.toUpperCase());
        return idx < 0 ? 0 : idx;
    }

    public static boolean canAccess(String userDensity, String dataDensity) {
        return DensityGuard.rank(userDensity) >= DensityGuard.rank(dataDensity);
    }

    public static void requireAccess(UserEntity user, String dataDensity) {
        if (!DensityGuard.canAccess(user.getDensity(), dataDensity)) {
            throw new BusinessException("\u65e0\u6743\u8bbf\u95ee\u8be5\u5bc6\u7ea7\u6570\u636e\uff08\u9700\u8981\u5bc6\u7ea7\uff1a" + DensityGuard.norm(dataDensity) + "\uff0c\u5f53\u524d\uff1a" + DensityGuard.norm(user.getDensity()) + "\uff09");
        }
    }

    public static String filterSql(String userDensity, String dataColumn) {
        return dataColumn + " in ('" + String.join((CharSequence)"','", DENSITY_ORDER.subList(0, DensityGuard.rank(userDensity) + 1)) + "')";
    }

    public static List<String> allowedDensities(String userDensity) {
        return DENSITY_ORDER.subList(0, DensityGuard.rank(userDensity) + 1);
    }

    private static String norm(String d) {
        return d == null || d.isBlank() ? "PUBLIC" : d.toUpperCase();
    }
}

