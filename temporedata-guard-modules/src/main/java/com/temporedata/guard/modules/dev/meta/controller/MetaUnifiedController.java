package com.temporedata.guard.modules.dev.meta.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.dev.meta.service.MetaUnifiedService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一元数据目录 API: 跨数据源(MySQL/ClickHouse/Iceberg/Kafka)元数据聚合。
 */
@RestController
@RequestMapping("/api/meta/unified")
@RequiredArgsConstructor
public class MetaUnifiedController {

    private final MetaUnifiedService metaUnifiedService;

    /** 全量同步(调各数据源插件 meta) */
    @PostMapping("/sync")
    public BaseResponse<Integer> sync(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaUnifiedService.syncAll(user.getTenantId()));
    }

    /** 统一目录查询 */
    @GetMapping("/list")
    public BaseResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal LoginUser user,
                                                        @RequestParam(required = false) String sourceType) {
        return BaseResponse.success(metaUnifiedService.list(user.getTenantId(), sourceType));
    }
}
