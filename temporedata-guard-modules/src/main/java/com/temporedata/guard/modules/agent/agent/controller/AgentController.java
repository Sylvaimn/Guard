package com.temporedata.guard.modules.agent.agent.controller;

import com.temporedata.guard.api.agent.AgentChatReq;
import com.temporedata.guard.api.agent.AgentChatRes;
import com.temporedata.guard.api.agent.AgentConfigReq;
import com.temporedata.guard.api.agent.AgentConfigRes;
import com.temporedata.guard.api.agent.AgentSessionRes;
import com.temporedata.guard.api.agent.AgentTestRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.agent.agent.service.AgentService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    public BaseResponse<AgentChatRes> chat(@RequestBody AgentChatReq req,
                                            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(agentService.chat(req, user.getTenantId(), user.getUserId(), user.getDensity()));
    }

    @GetMapping("/config")
    public BaseResponse<AgentConfigRes> getConfig(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(agentService.getConfig(user.getTenantId()));
    }

    /** 系统级模型策略(默认模型/参数约束/锁定开关/路由/配额) */
    @GetMapping("/model-policy")
    public BaseResponse<Map<String, Object>> getModelPolicy() {
        return BaseResponse.success(agentService.getModelPolicy());
    }

    /** 保存系统级模型策略(仅管理员) */
    @PostMapping("/model-policy")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Map<String, Object>> saveModelPolicy(@RequestBody Map<String, Object> policy) {
        return BaseResponse.success(agentService.saveModelPolicy(policy));
    }

    @PostMapping("/config")
    public BaseResponse<AgentConfigRes> saveConfig(@RequestBody AgentConfigReq req,
                                                   @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(agentService.saveConfig(req, user.getTenantId()));
    }

    @PostMapping("/config/test")
    public BaseResponse<AgentTestRes> testConfig(@RequestBody AgentConfigReq req) {
        return BaseResponse.success(agentService.testConnection(req));
    }

    @GetMapping("/sessions")
    public BaseResponse<List<AgentSessionRes>> listSessions(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(agentService.listSessions(user.getUserId()));
    }

    @GetMapping("/history/{sessionId}")
    public BaseResponse<List<Map<String, String>>> history(@PathVariable String sessionId) {
        return BaseResponse.success(agentService.history(sessionId));
    }

    @DeleteMapping("/session/{sessionId}")
    public BaseResponse<Void> deleteSession(@PathVariable String sessionId) {
        agentService.deleteSession(sessionId);
        return BaseResponse.success();
    }

    /** 会话置读(选中会话时调用, 清零未读) */
    @PostMapping("/session/{sessionId}/read")
    public BaseResponse<Void> markSessionRead(@PathVariable String sessionId) {
        agentService.markSessionRead(sessionId);
        return BaseResponse.success();
    }
}