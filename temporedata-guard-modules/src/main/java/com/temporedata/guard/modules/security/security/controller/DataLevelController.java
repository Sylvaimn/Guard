/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.security.DataLevelReq
 *  com.temporedata.guard.api.security.DataLevelRes
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.access.prepost.PreAuthorize
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
package com.temporedata.guard.modules.security.security.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.security.DataLevelReq;
import com.temporedata.guard.api.security.DataLevelRes;
import com.temporedata.guard.modules.security.security.service.DataLevelService;
import com.temporedata.guard.security.LoginUser;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping(value={"/api/security/level"})
public class DataLevelController {
    private final DataLevelService levelService;

    @GetMapping
    public BaseResponse<List<DataLevelRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.levelService.list(user.getTenantId()));
    }

    @GetMapping(value={"/{id}"})
    public BaseResponse<DataLevelRes> detail(@PathVariable String id) {
        return BaseResponse.success(this.levelService.detail(id));
    }

    @PostMapping
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<DataLevelRes> create(@RequestBody DataLevelReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.levelService.create(req, user.getTenantId()));
    }

    @PutMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<DataLevelRes> update(@PathVariable String id, @RequestBody DataLevelReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.levelService.update(id, req, user.getTenantId()));
    }

    @DeleteMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.levelService.delete(id);
        return BaseResponse.success();
    }

    public DataLevelController(DataLevelService levelService) {
        this.levelService = levelService;
    }
}

