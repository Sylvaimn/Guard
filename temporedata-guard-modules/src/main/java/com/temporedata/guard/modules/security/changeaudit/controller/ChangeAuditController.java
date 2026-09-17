/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.changeaudit.ChangeAuditRes
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.data.domain.Page
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.changeaudit.controller;
import com.temporedata.guard.modules.security.changeaudit.service.ChangeAuditService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.changeaudit.ChangeAuditRes;

import com.temporedata.guard.security.LoginUser;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/changeaudit"})
public class ChangeAuditController {
    private final ChangeAuditService changeAuditService;

    @GetMapping(value={"/list"})
    public BaseResponse<Page<ChangeAuditRes>> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String entityType, @RequestParam(required=false) String entityId, @RequestParam(required=false) String action, @RequestParam(required=false) String operator, @RequestParam(required=false) String startTime, @RequestParam(required=false) String endTime, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.changeAuditService.query(entityType, entityId, action, operator, startTime, endTime, user.getTenantId(), page, size));
    }

    @GetMapping(value={"/by-entity"})
    public BaseResponse<List<ChangeAuditRes>> listByEntity(@RequestParam String entityType, @RequestParam String entityId, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.changeAuditService.listByEntity(entityType, entityId, user.getTenantId()));
    }

    public ChangeAuditController(ChangeAuditService changeAuditService) {
        this.changeAuditService = changeAuditService;
    }
}

