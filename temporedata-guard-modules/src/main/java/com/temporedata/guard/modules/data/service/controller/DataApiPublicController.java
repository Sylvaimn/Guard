package com.temporedata.guard.modules.data.service.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.data.service.service.DataApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Public data API execution endpoint - no auth required, uses apiKey.
 * Accessible at /api/public/data/{path}?apiKey=xxx
 */
@RestController
@RequestMapping("/api/public/data")
@RequiredArgsConstructor
public class DataApiPublicController {

    private final DataApiService apiService;

    @GetMapping("/**")
    public BaseResponse<Object> executeGet(@RequestParam String apiKey,
                                            @RequestParam(required = false) Map<String, Object> params,
                                            HttpServletRequest request) {
        String path = extractPath(request);
        return BaseResponse.success(apiService.executeByKey(path, apiKey, params));
    }

    @PostMapping("/**")
    public BaseResponse<Object> executePost(@RequestParam String apiKey,
                                             @RequestBody(required = false) Map<String, Object> params,
                                             HttpServletRequest request) {
        String path = extractPath(request);
        return BaseResponse.success(apiService.executeByKey(path, apiKey, params));
    }

    private String extractPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/public/data/";
        String stripped = uri.substring(uri.indexOf(prefix) + prefix.length());
        return stripped.startsWith("/") ? stripped : "/" + stripped;
    }
}