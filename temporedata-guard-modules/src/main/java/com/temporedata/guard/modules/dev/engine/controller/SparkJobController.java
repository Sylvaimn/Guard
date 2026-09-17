package com.temporedata.guard.modules.dev.engine.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.engine.SparkJobReq;
import com.temporedata.guard.api.engine.SparkJobRes;
import com.temporedata.guard.modules.dev.engine.service.SparkJobService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/engine/spark")
@RequiredArgsConstructor
public class SparkJobController {

    private final SparkJobService sparkJobService;

    @GetMapping
    public BaseResponse<List<SparkJobRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(sparkJobService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<SparkJobRes> detail(@PathVariable String id) {
        return BaseResponse.success(sparkJobService.detail(id));
    }

    @PostMapping
    public BaseResponse<SparkJobRes> create(@RequestBody SparkJobReq req,
                                             @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(sparkJobService.create(req, user.getTenantId()));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        sparkJobService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<Void> execute(@PathVariable String id) {
        sparkJobService.executeAsync(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/kill")
    public BaseResponse<Void> kill(@PathVariable String id) {
        sparkJobService.kill(id);
        return BaseResponse.success();
    }

    @GetMapping("/{id}/logs")
    public BaseResponse<String> logs(@PathVariable String id) {
        return BaseResponse.success(sparkJobService.logs(id));
    }
}