package com.temporedata.guard.modules.platform.auth.controller;

import com.temporedata.guard.api.auth.LoginReq;
import com.temporedata.guard.api.auth.LoginRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.platform.auth.service.AuthService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authentication entry point.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public BaseResponse<LoginRes> login(@RequestBody LoginReq req) {
        return BaseResponse.success(authService.login(req));
    }

    /** 当前用户信息(工作台分流 + 权限兜底): 含 roles/dataScopes/securityLevel/workspace */
    @GetMapping("/me")
    public BaseResponse<Map<String, Object>> me(@AuthenticationPrincipal LoginUser user) {
        boolean isAdmin = user.getRoleCodes().stream()
                .anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("userId", user.getUserId());
        res.put("username", user.getUsername());
        res.put("tenantId", user.getTenantId());
        res.put("roles", new java.util.ArrayList<>(user.getRoleCodes()));
        res.put("density", user.getDensity());
        res.put("securityLevel", "L4");
        res.put("dataScopes", List.of("TENANT"));
        res.put("workspace", isAdmin ? "admin" : "user");
        res.put("projectScopes", List.of());
        return BaseResponse.success(res);
    }

    /** 操作权限集合(扁平化编码, 前端 v-permission 使用) */
    @GetMapping("/permissions")
    public BaseResponse<List<String>> permissions(@AuthenticationPrincipal LoginUser user) {
        Set<String> roles = user.getRoleCodes();
        boolean isAdmin = roles.stream().anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        java.util.Set<String> perms = new java.util.HashSet<>();
        if (isAdmin) {
            perms.add("*:*");
        } else {
            perms.addAll(List.of(
                    "workbench:view", "intelligence:view", "intelligence:detail", "intelligence:graph",
                    "project:view", "dataasset:view", "dataasset:query", "dataasset:apply",
                    "dataapi:view", "report:view", "form:view", "agent:view"));
        }
        return BaseResponse.success(new java.util.ArrayList<>(perms));
    }

    /** 数据范围声明 */
    @GetMapping("/data-scopes")
    public BaseResponse<Map<String, Object>> dataScopes(@AuthenticationPrincipal LoginUser user) {
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("scopeType", "TENANT");
        res.put("orgIds", List.of());
        res.put("projectIds", List.of());
        return BaseResponse.success(res);
    }

    /** 修改密码(校验旧密码; 修改后清除强制改密标记) */
    @PostMapping("/change-password")
    public BaseResponse<Void> changePassword(@RequestBody java.util.Map<String, String> body,
                                             @AuthenticationPrincipal LoginUser user) {
        authService.changePassword(user.getUsername(), body.get("oldPassword"), body.get("newPassword"));
        return BaseResponse.success();
    }
}