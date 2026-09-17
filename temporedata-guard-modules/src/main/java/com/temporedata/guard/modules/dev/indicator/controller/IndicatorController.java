package com.temporedata.guard.modules.dev.indicator.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.indicator.IndicatorLineageRes;
import com.temporedata.guard.api.indicator.IndicatorPageRes;
import com.temporedata.guard.api.indicator.IndicatorReq;
import com.temporedata.guard.api.indicator.IndicatorRes;
import com.temporedata.guard.api.indicator.IndicatorRunRes;
import com.temporedata.guard.api.indicator.IndicatorStatsRes;
import com.temporedata.guard.modules.dev.indicator.service.IndicatorService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Data indicator REST API.
 */
@RestController
@RequestMapping("/api/indicator")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;

    @GetMapping("/list")
    public BaseResponse<IndicatorPageRes> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.listAll(user.getTenantId(), page, size));
    }

    @GetMapping("/stats")
    public BaseResponse<IndicatorStatsRes> stats(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.stats(user.getTenantId()));
    }

    @PostMapping("/create")
    public BaseResponse<IndicatorRes> create(@RequestBody IndicatorReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<IndicatorRes> update(@PathVariable String id, @RequestBody IndicatorReq req,
                                             @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.update(id, req, user.getTenantId()));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        indicatorService.delete(id, user.getTenantId());
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<IndicatorRunRes> execute(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.execute(id, user.getTenantId()));
    }

    @GetMapping("/{id}/runs")
    public BaseResponse<List<IndicatorRunRes>> runs(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.listRuns(id, user.getTenantId()));
    }

    @GetMapping("/{id}/lineage")
    public BaseResponse<IndicatorLineageRes> lineage(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(indicatorService.lineage(id, user.getTenantId()));
    }
}