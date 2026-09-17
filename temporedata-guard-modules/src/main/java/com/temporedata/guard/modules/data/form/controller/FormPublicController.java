package com.temporedata.guard.modules.data.form.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.form.FormRes;
import com.temporedata.guard.api.form.FormSubmissionRes;
import com.temporedata.guard.modules.data.form.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Public form API (no authentication required).
 */
@RestController
@RequestMapping("/api/public/form")
@RequiredArgsConstructor
public class FormPublicController {

    private final FormService formService;

    @GetMapping("/shared/{shareToken}")
    public BaseResponse<FormRes> getByShareToken(@PathVariable String shareToken) {
        return BaseResponse.success(formService.getByShareToken(shareToken));
    }

    @PostMapping("/shared/{shareToken}/submit")
    public BaseResponse<FormSubmissionRes> submitByShareToken(
            @PathVariable String shareToken,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return BaseResponse.success(formService.submitByShareToken(shareToken, body.get("data"), ip));
    }
}