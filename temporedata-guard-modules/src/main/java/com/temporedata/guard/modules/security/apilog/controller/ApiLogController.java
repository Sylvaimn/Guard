/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.apilog.ApiLogReq
 *  com.temporedata.guard.api.apilog.ApiLogRes
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.data.domain.Page
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.apilog.controller;
import com.temporedata.guard.modules.security.apilog.service.ApiLogService;

import com.temporedata.guard.api.apilog.ApiLogReq;
import com.temporedata.guard.api.apilog.ApiLogRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;

import com.temporedata.guard.security.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/apilog"})
public class ApiLogController {
    private final ApiLogService apiLogService;

    @GetMapping(value={"/list"})
    public BaseResponse<Page<ApiLogRes>> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.apiLogService.listAll(page, size, user.getTenantId()));
    }

    @PostMapping(value={"/log"})
    public BaseResponse<Void> log(@RequestBody ApiLogReq req, @AuthenticationPrincipal LoginUser user) {
        this.apiLogService.log(req, user.getTenantId());
        return BaseResponse.success();
    }

    @DeleteMapping(value={"/clear"})
    public BaseResponse<Void> clear(@RequestParam(defaultValue="30") int daysBefore) {
        this.apiLogService.clearOldLogs(daysBefore);
        return BaseResponse.success();
    }

    public ApiLogController(ApiLogService apiLogService) {
        this.apiLogService = apiLogService;
    }
}

