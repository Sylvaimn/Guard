package com.temporedata.guard.modules.platform.license.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.license.LicenseInstallReq;
import com.temporedata.guard.api.license.LicenseRes;
import com.temporedata.guard.modules.platform.license.service.LicenseService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * License management REST API.
 */
@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping
    public BaseResponse<LicenseRes> getLicense(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(licenseService.getLicense(user.getTenantId()));
    }

    @PostMapping("/install")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<LicenseRes> install(@RequestBody LicenseInstallReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(licenseService.installLicense(req, user.getTenantId()));
    }

    @GetMapping("/status")
    public BaseResponse<LicenseRes> status(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(licenseService.checkLicense(user.getTenantId()));
    }
}