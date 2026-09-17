/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.api.security.MaskRuleReq
 *  com.temporedata.guard.api.security.MaskRuleRes
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
import com.temporedata.guard.api.security.MaskRuleReq;
import com.temporedata.guard.api.security.MaskRuleRes;
import com.temporedata.guard.modules.security.security.service.MaskService;
import com.temporedata.guard.security.LoginUser;
import java.util.List;
import java.util.Map;
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
@RequestMapping(value={"/api/security/mask"})
public class MaskController {
    private final MaskService maskService;

    @GetMapping
    public BaseResponse<List<MaskRuleRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.maskService.list(user.getTenantId()));
    }

    @GetMapping(value={"/{id}"})
    public BaseResponse<MaskRuleRes> detail(@PathVariable String id) {
        return BaseResponse.success(this.maskService.detail(id));
    }

    @PostMapping
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<MaskRuleRes> create(@RequestBody MaskRuleReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.maskService.create(req, user.getTenantId()));
    }

    @PutMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<MaskRuleRes> update(@PathVariable String id, @RequestBody MaskRuleReq req) {
        return BaseResponse.success(this.maskService.update(id, req));
    }

    @DeleteMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.maskService.delete(id);
        return BaseResponse.success();
    }

    @PutMapping(value={"/{id}/toggle"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<MaskRuleRes> toggle(@PathVariable String id) {
        return BaseResponse.success(this.maskService.toggle(id));
    }

    @PostMapping(value={"/test"})
    public BaseResponse<Map<String, String>> testMask(@RequestBody Map<String, String> params) {
        String value = params.get("value");
        String ruleType = params.get("ruleType");
        String masked = this.maskService.maskValue(value, ruleType);
        return BaseResponse.success(Map.of("original", value, "masked", masked, "ruleType", ruleType));
    }

    public MaskController(MaskService maskService) {
        this.maskService = maskService;
    }
}

