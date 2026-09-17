package com.temporedata.guard.modules.data.form.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.form.FormReq;
import com.temporedata.guard.api.form.FormRes;
import com.temporedata.guard.api.form.FormSubmissionRes;
import com.temporedata.guard.modules.data.form.service.FormService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Form management REST API (authenticated).
 */
@RestController
@RequestMapping("/api/form")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    @GetMapping("/list")
    public BaseResponse<List<FormRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(formService.listAll(user.getTenantId()));
    }

    @PostMapping("/create")
    public BaseResponse<FormRes> create(@RequestBody FormReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(formService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<FormRes> update(@PathVariable String id, @RequestBody FormReq req) {
        return BaseResponse.success(formService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        formService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/{id}/submissions")
    public BaseResponse<List<FormSubmissionRes>> listSubmissions(@PathVariable String id) {
        return BaseResponse.success(formService.listSubmissions(id));
    }

    @PostMapping("/{id}/share-token")
    public BaseResponse<FormRes> generateShareToken(@PathVariable String id) {
        return BaseResponse.success(formService.generateShareToken(id));
    }
}