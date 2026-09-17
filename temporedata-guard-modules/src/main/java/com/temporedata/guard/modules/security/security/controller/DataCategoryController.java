/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.security.DataCategoryReq
 *  com.temporedata.guard.api.security.DataCategoryRes
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
import com.temporedata.guard.api.security.DataCategoryReq;
import com.temporedata.guard.api.security.DataCategoryRes;
import com.temporedata.guard.modules.security.security.service.DataCategoryService;
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
@RequestMapping(value={"/api/security/category"})
public class DataCategoryController {
    private final DataCategoryService categoryService;

    @GetMapping(value={"/tree"})
    public BaseResponse<List<DataCategoryRes>> tree(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.categoryService.tree(user.getTenantId()));
    }

    @PostMapping
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<DataCategoryRes> create(@RequestBody DataCategoryReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.categoryService.create(req, user.getTenantId()));
    }

    @PutMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<DataCategoryRes> update(@PathVariable String id, @RequestBody DataCategoryReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.categoryService.update(id, req, user.getTenantId()));
    }

    @DeleteMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.categoryService.delete(id);
        return BaseResponse.success();
    }

    public DataCategoryController(DataCategoryService categoryService) {
        this.categoryService = categoryService;
    }
}

