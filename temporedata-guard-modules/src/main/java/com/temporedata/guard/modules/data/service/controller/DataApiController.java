package com.temporedata.guard.modules.data.service.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.service.DataApiReq;
import com.temporedata.guard.api.service.DataApiRes;
import com.temporedata.guard.api.service.DataApiTestReq;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.data.service.service.DataApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Data API controller: publish SQL queries as REST endpoints.
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class DataApiController {

    private final DataApiService apiService;

    @GetMapping
    public BaseResponse<List<DataApiRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(apiService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<DataApiRes> detail(@PathVariable String id) {
        return BaseResponse.success(apiService.detail(id));
    }

    @PostMapping
    public BaseResponse<DataApiRes> create(@RequestBody DataApiReq req,
                                            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(apiService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<DataApiRes> update(@PathVariable String id, @RequestBody DataApiReq req) {
        return BaseResponse.success(apiService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        apiService.delete(id);
        return BaseResponse.success();
    }

    @PutMapping("/{id}/submit-approval")
    public BaseResponse<DataApiRes> submitApproval(@PathVariable String id) {
        return BaseResponse.success(apiService.submitForApproval(id));
    }

    @PutMapping("/{id}/approve")
    public BaseResponse<DataApiRes> approve(@PathVariable String id) {
        return BaseResponse.success(apiService.approve(id));
    }

    @PutMapping("/{id}/reject")
    public BaseResponse<DataApiRes> reject(@PathVariable String id) {
        return BaseResponse.success(apiService.reject(id));
    }

    @PutMapping("/{id}/toggle")
    public BaseResponse<DataApiRes> toggleStatus(@PathVariable String id) {
        return BaseResponse.success(apiService.toggleStatus(id));
    }

    @PutMapping("/{id}/regenerate-key")
    public BaseResponse<DataApiRes> regenerateKey(@PathVariable String id) {
        return BaseResponse.success(apiService.regenerateKey(id));
    }

    @PostMapping("/{id}/test")
    public BaseResponse<Object> testExecute(@PathVariable String id,
                                             @RequestBody DataApiTestReq req) {
        return BaseResponse.success(apiService.testExecute(id, req));
    }
}