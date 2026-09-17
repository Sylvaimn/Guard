/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.behaviorlog.BehaviorLogRes
 *  com.temporedata.guard.api.loginlog.LoginLogRes
 *  com.temporedata.guard.api.security.AuditLogReq
 *  com.temporedata.guard.api.security.AuditLogRes
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.data.domain.Page
 *  org.springframework.security.access.prepost.PreAuthorize
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.security.controller;
import com.temporedata.guard.modules.security.behaviorlog.service.BehaviorLogService;
import com.temporedata.guard.modules.security.loginlog.service.LoginLogService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.behaviorlog.BehaviorLogRes;
import com.temporedata.guard.api.loginlog.LoginLogRes;
import com.temporedata.guard.api.security.AuditLogReq;
import com.temporedata.guard.api.security.AuditLogRes;


import com.temporedata.guard.modules.security.security.service.AuditLogService;
import com.temporedata.guard.security.LoginUser;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/security/audit"})
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final LoginLogService loginLogService;
    private final BehaviorLogService behaviorLogService;

    @PostMapping(value={"/query"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<Page<AuditLogRes>> query(@RequestBody AuditLogReq req, @AuthenticationPrincipal LoginUser user, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return BaseResponse.success(this.auditLogService.query(req, user.getTenantId(), page, size));
    }

    @PostMapping(value={"/record"})
    public BaseResponse<Void> record(@RequestBody Map<String, String> params, @AuthenticationPrincipal LoginUser user) {
        this.auditLogService.record(user.getUsername(), params.getOrDefault("actionType", "QUERY"), params.getOrDefault("actionDetail", ""), params.getOrDefault("targetType", ""), params.getOrDefault("targetName", ""), params.getOrDefault("ipAddress", ""), params.getOrDefault("result", "SUCCESS"), params.getOrDefault("errorMsg", null), user.getTenantId());
        return BaseResponse.success();
    }

    @DeleteMapping(value={"/clear"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<Void> clear(@AuthenticationPrincipal LoginUser user) {
        this.auditLogService.deleteAll(user.getTenantId());
        return BaseResponse.success();
    }

    @GetMapping(value={"/login-logs"})
    public BaseResponse<Page<LoginLogRes>> loginLogs(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.loginLogService.listAll(page, size, user.getTenantId()));
    }

    @GetMapping(value={"/behavior-logs"})
    public BaseResponse<Page<BehaviorLogRes>> behaviorLogs(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.behaviorLogService.listAll(page, size, user.getTenantId()));
    }

    public AuditLogController(AuditLogService auditLogService, LoginLogService loginLogService, BehaviorLogService behaviorLogService) {
        this.auditLogService = auditLogService;
        this.loginLogService = loginLogService;
        this.behaviorLogService = behaviorLogService;
    }
}

