/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.sensitive.SensitiveDataReq
 *  com.temporedata.guard.api.sensitive.SensitiveDataRes
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.sensitive.controller;
import com.temporedata.guard.modules.security.sensitive.service.SensitiveDataService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.sensitive.SensitiveDataReq;
import com.temporedata.guard.api.sensitive.SensitiveDataRes;

import com.temporedata.guard.security.LoginUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/sensitive"})
public class SensitiveDataController {
    private final SensitiveDataService sensitiveDataService;

    @GetMapping(value={"/list"})
    public BaseResponse<List<SensitiveDataRes>> list() {
        return BaseResponse.success(this.sensitiveDataService.listAll());
    }

    @PostMapping(value={"/create"})
    public BaseResponse<SensitiveDataRes> create(@RequestBody SensitiveDataReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.sensitiveDataService.create(req, user.getTenantId()));
    }

    @PutMapping(value={"/{id}"})
    public BaseResponse<SensitiveDataRes> update(@PathVariable String id, @RequestBody SensitiveDataReq req) {
        return BaseResponse.success(this.sensitiveDataService.update(id, req));
    }

    @DeleteMapping(value={"/{id}"})
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.sensitiveDataService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping(value={"/discover"})
    public BaseResponse<List<SensitiveDataRes>> discover(@RequestParam String datasourceId, @RequestParam String tableName, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.sensitiveDataService.discover(datasourceId, tableName, user.getTenantId()));
    }

    @PostMapping(value={"/apply-mask"})
    public BaseResponse<List<SensitiveDataRes>> applyMask(@RequestParam String datasourceId, @RequestParam String tableName, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.sensitiveDataService.applyMask(datasourceId, tableName, user.getTenantId()));
    }

    public SensitiveDataController(SensitiveDataService sensitiveDataService) {
        this.sensitiveDataService = sensitiveDataService;
    }
}

