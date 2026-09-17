package com.temporedata.guard.modules.dev.query.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.query.QueryLogRes;
import com.temporedata.guard.api.query.QueryReq;
import com.temporedata.guard.api.query.QueryRes;
import com.temporedata.guard.modules.dev.query.service.QueryService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping("/execute")
    public BaseResponse<QueryRes> execute(@RequestBody QueryReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(queryService.execute(req, user.getUserId(), user.getTenantId()));
    }

    @GetMapping("/history")
    public BaseResponse<List<QueryLogRes>> history(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(queryService.history(user.getTenantId()));
    }
}