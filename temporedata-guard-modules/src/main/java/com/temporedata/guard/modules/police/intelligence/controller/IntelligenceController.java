package com.temporedata.guard.modules.police.intelligence.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    /** 情报检索：姓名/主标识/标签，行级密级过滤 + 脱敏 */
    @GetMapping("/search")
    public BaseResponse<List<IntelligenceRes>> search(@RequestParam String keyword,
                                                      @RequestParam(required = false) String labelCode,
                                                      @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(intelligenceService.search(keyword, labelCode, user));
    }

    /** Agent RAG 上下文 */
    @GetMapping("/rag")
    public BaseResponse<String> rag(@RequestParam String keyword, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(intelligenceService.buildRagContext(keyword, user.getTenantId(), user.getDensity()));
    }
}
