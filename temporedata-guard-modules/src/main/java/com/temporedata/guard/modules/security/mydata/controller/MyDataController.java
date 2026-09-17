/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.mydata.MyDataGrantReq
 *  com.temporedata.guard.api.mydata.MyDataReq
 *  com.temporedata.guard.api.mydata.MyDataRes
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.mydata.controller;
import com.temporedata.guard.modules.security.mydata.service.MyDataService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.mydata.MyDataGrantReq;
import com.temporedata.guard.api.mydata.MyDataReq;
import com.temporedata.guard.api.mydata.MyDataRes;

import com.temporedata.guard.security.LoginUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/mydata"})
public class MyDataController {
    private final MyDataService myDataService;

    @GetMapping(value={"/list"})
    public BaseResponse<List<MyDataRes>> list(@RequestParam(required=false) String resourceType, @AuthenticationPrincipal LoginUser user) {
        if (resourceType != null && !resourceType.isEmpty()) {
            return BaseResponse.success(this.myDataService.listByUserAndType(user.getUserId(), resourceType));
        }
        return BaseResponse.success(this.myDataService.listByUser(user.getUserId()));
    }

    @PostMapping(value={"/favorite"})
    public BaseResponse<MyDataRes> addFavorite(@RequestBody MyDataReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.myDataService.addFavorite(user.getUserId(), req.getResourceType(), req.getResourceId(), req.getResourceName(), user.getTenantId()));
    }

    @DeleteMapping(value={"/favorite/{resourceId}"})
    public BaseResponse<Void> removeFavorite(@PathVariable String resourceId, @AuthenticationPrincipal LoginUser user) {
        this.myDataService.removeFavorite(user.getUserId(), resourceId);
        return BaseResponse.success();
    }

    @PostMapping(value={"/grant"})
    public BaseResponse<MyDataRes> grantAccess(@RequestBody MyDataGrantReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.myDataService.grantAccess(req, user.getTenantId(), user.getUserId()));
    }

    public MyDataController(MyDataService myDataService) {
        this.myDataService = myDataService;
    }
}

