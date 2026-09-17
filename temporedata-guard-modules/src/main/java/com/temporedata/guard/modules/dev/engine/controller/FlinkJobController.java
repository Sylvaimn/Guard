package com.temporedata.guard.modules.dev.engine.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.engine.FlinkJobReq;
import com.temporedata.guard.api.engine.FlinkJobRes;
import com.temporedata.guard.modules.dev.engine.service.FlinkJobService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/engine/flink")
@RequiredArgsConstructor
public class FlinkJobController {

    private final FlinkJobService flinkJobService;

    @GetMapping
    public BaseResponse<List<FlinkJobRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(flinkJobService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<FlinkJobRes> detail(@PathVariable String id) {
        return BaseResponse.success(flinkJobService.detail(id));
    }

    @PostMapping
    public BaseResponse<FlinkJobRes> create(@RequestBody FlinkJobReq req,
                                             @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(flinkJobService.create(req, user.getTenantId()));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        flinkJobService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<Void> execute(@PathVariable String id) {
        flinkJobService.executeAsync(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/cancel")
    public BaseResponse<Void> cancel(@PathVariable String id) {
        flinkJobService.cancel(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/savepoint")
    public BaseResponse<String> savepoint(@PathVariable String id) {
        return BaseResponse.success(flinkJobService.savepoint(id));
    }
}