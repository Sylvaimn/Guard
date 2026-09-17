package com.temporedata.guard.modules.platform.preference.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.preference.PreferenceBatchReq;
import com.temporedata.guard.api.preference.PreferenceReq;
import com.temporedata.guard.api.preference.PreferenceRes;
import com.temporedata.guard.modules.platform.preference.service.PreferenceService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User preference REST API.
 */
@RestController
@RequestMapping("/api/preference")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping("/list")
    public BaseResponse<List<PreferenceRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(preferenceService.getByUserId(user.getUserId()));
    }

    @PostMapping("/save")
    public BaseResponse<PreferenceRes> save(@RequestBody PreferenceReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(preferenceService.saveOrUpdate(user.getUserId(), req.getPrefKey(), req.getPrefValue()));
    }

    @PostMapping("/batch-save")
    public BaseResponse<Void> batchSave(@RequestBody PreferenceBatchReq req, @AuthenticationPrincipal LoginUser user) {
        preferenceService.batchSave(user.getUserId(), req.getPreferences());
        return BaseResponse.success();
    }
}