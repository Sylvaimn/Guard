package com.temporedata.guard.modules.dev.quality.controller;
import com.temporedata.guard.modules.dev.quality.service.QualityScheduleService;
import com.temporedata.guard.modules.dev.quality.service.QualityService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.quality.QualityCheckRes;
import com.temporedata.guard.api.quality.QualityRuleReq;
import com.temporedata.guard.api.quality.QualityRuleRes;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;
    private final QualityScheduleService qualityScheduleService;

    @GetMapping
    public BaseResponse<List<QualityRuleRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(qualityService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<QualityRuleRes> detail(@PathVariable String id) {
        return BaseResponse.success(qualityService.detail(id));
    }

    /** 实时质量检查记录(CDC 流式校验结果, REALTIME 类型, 最近 50 条) */
    @GetMapping("/check/realtime")
    public BaseResponse<java.util.List<java.util.Map<String, Object>>> realtimeChecks(
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(qualityService.realtimeChecks());
    }

    @PostMapping
    public BaseResponse<QualityRuleRes> create(@RequestBody QualityRuleReq req,
                                                @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(qualityService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<QualityRuleRes> update(@PathVariable String id, @RequestBody QualityRuleReq req) {
        return BaseResponse.success(qualityService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        qualityService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<QualityCheckRes> execute(@PathVariable String id) {
        return BaseResponse.success(qualityService.execute(id));
    }

    /** 配置定时质量检查(cron) */
    @PostMapping("/{id}/schedule")
    public BaseResponse<Void> schedule(@PathVariable String id, @RequestParam String cron) {
        qualityService.updateSchedule(id, cron);
        qualityScheduleService.scheduleRule(id, cron);
        return BaseResponse.success();
    }

    /** 取消定时质量检查 */
    @PostMapping("/{id}/unschedule")
    public BaseResponse<Void> unschedule(@PathVariable String id) {
        qualityService.updateSchedule(id, null);
        qualityScheduleService.removeSchedule(id);
        return BaseResponse.success();
    }
}