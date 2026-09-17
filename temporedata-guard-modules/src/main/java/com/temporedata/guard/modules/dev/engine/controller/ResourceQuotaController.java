package com.temporedata.guard.modules.dev.engine.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.dev.engine.service.ResourceQuotaService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资源配额 API: 租户级作业资源限额查询/更新。
 */
@RestController
@RequestMapping("/api/engine/quota")
@RequiredArgsConstructor
public class ResourceQuotaController {

    private final ResourceQuotaService quotaService;

    /** 当前租户配额 */
    @GetMapping
    public BaseResponse<Map<String, Object>> quota(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(quotaService.quotaView(user.getTenantId()));
    }

    /** 更新配额 */
    @PutMapping
    public BaseResponse<Map<String, Object>> update(@AuthenticationPrincipal LoginUser user,
                                                    @RequestBody Map<String, Integer> body) {
        quotaService.updateQuota(user.getTenantId(),
                body.get("maxExecutors"), body.get("maxMemoryGb"),
                body.get("maxRuntimeMin"), body.get("maxConcurrency"));
        return BaseResponse.success(quotaService.quotaView(user.getTenantId()));
    }
}
