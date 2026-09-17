/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.permapproval.PermApprovalReq
 *  com.temporedata.guard.api.permapproval.PermApprovalRes
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.permapproval.controller;
import com.temporedata.guard.modules.security.permapproval.service.PermApprovalService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.permapproval.PermApprovalReq;
import com.temporedata.guard.api.permapproval.PermApprovalRes;

import com.temporedata.guard.security.LoginUser;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/permapproval"})
public class PermApprovalController {
    private final PermApprovalService permApprovalService;

    @GetMapping(value={"/my"})
    public BaseResponse<List<PermApprovalRes>> myApplications(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.permApprovalService.listByApplicant(user.getUserId()));
    }

    @GetMapping(value={"/pending"})
    public BaseResponse<List<PermApprovalRes>> pending() {
        return BaseResponse.success(this.permApprovalService.listPending());
    }

    @PostMapping(value={"/create"})
    public BaseResponse<PermApprovalRes> create(@RequestBody PermApprovalReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.permApprovalService.create(req, user.getUserId(), user.getUsername(), user.getTenantId()));
    }

    @PostMapping(value={"/{id}/approve"})
    public BaseResponse<PermApprovalRes> approve(@PathVariable String id, @RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.permApprovalService.approve(id, user.getUserId(), user.getUsername(), body.get("comment")));
    }

    @PostMapping(value={"/{id}/reject"})
    public BaseResponse<PermApprovalRes> reject(@PathVariable String id, @RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.permApprovalService.reject(id, user.getUserId(), user.getUsername(), body.get("comment")));
    }

    public PermApprovalController(PermApprovalService permApprovalService) {
        this.permApprovalService = permApprovalService;
    }
}

