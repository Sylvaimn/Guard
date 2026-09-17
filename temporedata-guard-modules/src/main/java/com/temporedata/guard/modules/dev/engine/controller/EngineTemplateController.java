package com.temporedata.guard.modules.dev.engine.service;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 湖仓作业模板: 预置 Spark/Flink 湖仓作业骨架, 一键从模板创建引擎作业。
 */
@RestController
@RequestMapping("/api/engine/templates")
@RequiredArgsConstructor
public class EngineTemplateController {

    private final EngineTemplateService templateService;

    /** 模板列表(预置 5 类湖仓作业) */
    @GetMapping
    public BaseResponse<List<Map<String, Object>>> templates(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(templateService.list());
    }

    /** 从模板创建作业(生成 SQL 骨架 + 落库为 Spark/Flink 作业) */
    @PostMapping("/{templateId}/create")
    public BaseResponse<String> createFromTemplate(@PathVariable String templateId,
                                                   @RequestBody Map<String, String> params,
                                                   @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(templateService.createJob(templateId, params, user.getTenantId()));
    }
}
