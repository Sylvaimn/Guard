package com.temporedata.guard.modules.platform.passwordless.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.passwordless.PasswordlessConfigReq;
import com.temporedata.guard.api.passwordless.PasswordlessConfigRes;
import com.temporedata.guard.api.passwordless.PasswordlessSendReq;
import com.temporedata.guard.api.passwordless.PasswordlessVerifyReq;
import com.temporedata.guard.modules.platform.passwordless.entity.PasswordlessConfigEntity;
import com.temporedata.guard.modules.platform.passwordless.service.PasswordlessService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Passwordless login REST API.
 */
@RestController
@RequestMapping("/api/passwordless")
@RequiredArgsConstructor
public class PasswordlessController {

    private final PasswordlessService passwordlessService;

    @GetMapping("/list")
    public BaseResponse<List<PasswordlessConfigRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(passwordlessService.listAll(user.getTenantId()));
    }

    @PostMapping("/create")
    public BaseResponse<PasswordlessConfigRes> create(@RequestBody PasswordlessConfigReq req,
                                                       @AuthenticationPrincipal LoginUser user) {
        PasswordlessConfigEntity entity = new PasswordlessConfigEntity();
        entity.setLoginType(req.getLoginType());
        entity.setConfig(req.getConfig());
        entity.setTenantId(user.getTenantId());
        return BaseResponse.success(passwordlessService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<PasswordlessConfigRes> update(@PathVariable String id, @RequestBody PasswordlessConfigReq req) {
        PasswordlessConfigEntity entity = new PasswordlessConfigEntity();
        entity.setLoginType(req.getLoginType());
        entity.setConfig(req.getConfig());
        return BaseResponse.success(passwordlessService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        passwordlessService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/enable")
    public BaseResponse<PasswordlessConfigRes> enable(@PathVariable String id) {
        return BaseResponse.success(passwordlessService.enable(id));
    }

    @PostMapping("/{id}/disable")
    public BaseResponse<PasswordlessConfigRes> disable(@PathVariable String id) {
        return BaseResponse.success(passwordlessService.disable(id));
    }

    @PostMapping("/send-code")
    public BaseResponse<Map<String, Boolean>> sendCode(@RequestBody PasswordlessSendReq req) {
        passwordlessService.sendCode(req.getLoginType(), req.getTarget());
        return BaseResponse.success(Map.of("success", true));
    }

    @PostMapping("/verify")
    public BaseResponse<Map<String, Boolean>> verify(@RequestBody PasswordlessVerifyReq req) {
        boolean verified = passwordlessService.verifyCode(req.getLoginType(), req.getTarget(), req.getCode());
        return BaseResponse.success(Map.of("verified", verified));
    }
}