/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.blacklist.BlacklistReq
 *  com.temporedata.guard.api.blacklist.BlacklistRes
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
package com.temporedata.guard.modules.security.blacklist.controller;
import com.temporedata.guard.modules.security.blacklist.service.BlacklistService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.blacklist.BlacklistReq;
import com.temporedata.guard.api.blacklist.BlacklistRes;

import com.temporedata.guard.modules.security.security.audit.Audit;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/blacklist"})
public class BlacklistController {
    private final BlacklistService blacklistService;

    @GetMapping(value={"/list"})
    public BaseResponse<List<BlacklistRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.blacklistService.listAll(user.getTenantId()));
    }

    @Audit(action="CREATE", targetType="BLACKLIST", targetName="#{#req.ipAddress}", detail="\u7c7b\u578b #{#req.listType}")
    @PostMapping(value={"/create"})
    public BaseResponse<BlacklistRes> create(@RequestBody BlacklistReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.blacklistService.create(req, user.getTenantId()));
    }

    @Audit(action="UPDATE", targetType="BLACKLIST", targetName="#{#id}")
    @PutMapping(value={"/{id}"})
    public BaseResponse<BlacklistRes> update(@PathVariable String id, @RequestBody BlacklistReq req) {
        return BaseResponse.success(this.blacklistService.update(id, req));
    }

    @Audit(action="DELETE", targetType="BLACKLIST", targetName="#{#id}")
    @DeleteMapping(value={"/{id}"})
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.blacklistService.delete(id);
        return BaseResponse.success();
    }

    @Audit(action="UPDATE", targetType="BLACKLIST", targetName="#{#id}", detail="\u542f\u7528")
    @PostMapping(value={"/{id}/enable"})
    public BaseResponse<BlacklistRes> enable(@PathVariable String id) {
        return BaseResponse.success(this.blacklistService.enable(id));
    }

    @Audit(action="UPDATE", targetType="BLACKLIST", targetName="#{#id}", detail="\u505c\u7528")
    @PostMapping(value={"/{id}/disable"})
    public BaseResponse<BlacklistRes> disable(@PathVariable String id) {
        return BaseResponse.success(this.blacklistService.disable(id));
    }

    @GetMapping(value={"/check"})
    public BaseResponse<Map<String, Boolean>> check(@RequestParam String ip) {
        boolean blocked = this.blacklistService.isBlocked(ip);
        boolean whitelisted = this.blacklistService.isWhitelisted(ip);
        return BaseResponse.success(Map.of("blocked", blocked, "whitelisted", whitelisted));
    }

    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }
}

