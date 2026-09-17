package com.temporedata.guard.modules.platform.user.controller;

import com.temporedata.guard.api.auth.UserInfoRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.security.context.TenantContext;
import com.temporedata.guard.modules.platform.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * User-facing endpoints. Protected; admin-only endpoint demonstrates RBAC.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final TenantService tenantService;

    @GetMapping("/me")
    public BaseResponse<UserInfoRes> me(@AuthenticationPrincipal LoginUser loginUser) {
        String tenantName = tenantService.getTenantName(loginUser.getTenantId());
        UserInfoRes user = new UserInfoRes(loginUser.getUserId(), loginUser.getUsername(),
                loginUser.getTenantId(), tenantName, new ArrayList<>(loginUser.getRoleCodes()),
                loginUser.getDensity());
        return BaseResponse.success(user);
    }

    @GetMapping("/ping")
    public BaseResponse<String> ping() {
        return BaseResponse.success("pong, tenant=" + TenantContext.getTenantId());
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<String> adminOnly() {
        return BaseResponse.success("这是管理员接口");
    }
}