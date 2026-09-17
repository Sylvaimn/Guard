package com.temporedata.guard.modules.platform.secret.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.secret.SecretReq;
import com.temporedata.guard.api.secret.SecretRes;
import com.temporedata.guard.modules.platform.secret.service.SecretService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Secret management REST API.
 */
@RestController
@RequestMapping("/api/secret")
@RequiredArgsConstructor
public class SecretController {

    private final SecretService secretService;

    @GetMapping
    public BaseResponse<List<SecretRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(secretService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<SecretRes> detail(@PathVariable String id) {
        return BaseResponse.success(secretService.detail(id));
    }

    @PostMapping
    public BaseResponse<SecretRes> create(@RequestBody SecretReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(secretService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<SecretRes> update(@PathVariable String id, @RequestBody SecretReq req) {
        return BaseResponse.success(secretService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        secretService.delete(id);
        return BaseResponse.success();
    }
}