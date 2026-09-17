package com.temporedata.guard.modules.dev.real.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.real.RealJobReq;
import com.temporedata.guard.api.real.RealJobRes;
import com.temporedata.guard.modules.dev.real.service.RealService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Realtime computing REST API.
 */
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealController {

    private final RealService realService;

    @GetMapping
    public BaseResponse<List<RealJobRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(realService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<RealJobRes> detail(@PathVariable String id) {
        return BaseResponse.success(realService.detail(id));
    }

    @PostMapping
    public BaseResponse<RealJobRes> create(@RequestBody RealJobReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(realService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<RealJobRes> update(@PathVariable String id, @RequestBody RealJobReq req) {
        return BaseResponse.success(realService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        realService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/start")
    public BaseResponse<Void> start(@PathVariable String id) {
        realService.startJob(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/stop")
    public BaseResponse<Void> stop(@PathVariable String id) {
        realService.stopJob(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/savepoint")
    public BaseResponse<String> savepoint(@PathVariable String id) {
        return BaseResponse.success(realService.triggerSavepoint(id));
    }

    @GetMapping("/{id}/logs")
    public BaseResponse<String> logs(@PathVariable String id) {
        return BaseResponse.success(realService.getLogs(id));
    }
}