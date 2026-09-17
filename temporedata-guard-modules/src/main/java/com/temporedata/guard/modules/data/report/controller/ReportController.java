package com.temporedata.guard.modules.data.report.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.report.ReportReq;
import com.temporedata.guard.api.report.ReportRes;
import com.temporedata.guard.modules.data.report.service.ReportService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Data report REST API.
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/list")
    public BaseResponse<List<ReportRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(reportService.listAll(user.getTenantId()));
    }

    @PostMapping("/create")
    public BaseResponse<ReportRes> create(@RequestBody ReportReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(reportService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<ReportRes> update(@PathVariable String id, @RequestBody ReportReq req) {
        return BaseResponse.success(reportService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        reportService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/publish")
    public BaseResponse<ReportRes> publish(@PathVariable String id) {
        return BaseResponse.success(reportService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    public BaseResponse<ReportRes> unpublish(@PathVariable String id) {
        return BaseResponse.success(reportService.unpublish(id));
    }
}