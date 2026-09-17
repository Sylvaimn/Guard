package com.temporedata.guard.modules.dev.dashboard.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.dashboard.DashboardRes;
import com.temporedata.guard.modules.dev.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard controller: aggregated overview stats.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public BaseResponse<DashboardRes> overview() {
        return BaseResponse.success(dashboardService.overview());
    }
}