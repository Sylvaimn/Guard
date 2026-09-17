package com.temporedata.guard.modules.dev.func.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.func.FuncReq;
import com.temporedata.guard.api.func.FuncRes;
import com.temporedata.guard.modules.dev.func.service.FuncService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/func")
@RequiredArgsConstructor
public class FuncController {

    private final FuncService funcService;

    @GetMapping
    public BaseResponse<List<FuncRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(funcService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<FuncRes> detail(@PathVariable String id) {
        return BaseResponse.success(funcService.detail(id));
    }

    @PostMapping
    public BaseResponse<FuncRes> create(@RequestBody FuncReq req,
                                         @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(funcService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<FuncRes> update(@PathVariable String id, @RequestBody FuncReq req) {
        return BaseResponse.success(funcService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        funcService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/test")
    public BaseResponse<String> test(@PathVariable String id) {
        return BaseResponse.success(funcService.testFunc(id));
    }
}