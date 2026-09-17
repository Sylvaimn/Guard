/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.approval.ApprovalReq
 *  com.temporedata.guard.api.approval.ApprovalRes
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.approval.controller;
import com.temporedata.guard.modules.security.approval.service.ApprovalService;

import com.temporedata.guard.api.approval.ApprovalReq;
import com.temporedata.guard.api.approval.ApprovalRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;

import com.temporedata.guard.security.LoginUser;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/approval"})
public class ApprovalController {
    private final ApprovalService approvalService;

    @GetMapping(value={"/list"})
    public BaseResponse<List<ApprovalRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.approvalService.listAll(user.getTenantId()));
    }

    @PostMapping(value={"/create"})
    public BaseResponse<ApprovalRes> create(@RequestBody ApprovalReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.approvalService.create(req, user.getTenantId()));
    }

    @PutMapping(value={"/{id}"})
    public BaseResponse<ApprovalRes> update(@PathVariable String id, @RequestBody ApprovalReq req) {
        return BaseResponse.success(this.approvalService.update(id, req));
    }

    @DeleteMapping(value={"/{id}"})
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.approvalService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping(value={"/{id}/approve"})
    public BaseResponse<ApprovalRes> approve(@PathVariable String id, @RequestBody Map<String, String> body) {
        return BaseResponse.success(this.approvalService.approve(id, body.get("comment")));
    }

    @PostMapping(value={"/{id}/reject"})
    public BaseResponse<ApprovalRes> reject(@PathVariable String id, @RequestBody Map<String, String> body) {
        return BaseResponse.success(this.approvalService.reject(id, body.get("comment")));
    }

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }
}

