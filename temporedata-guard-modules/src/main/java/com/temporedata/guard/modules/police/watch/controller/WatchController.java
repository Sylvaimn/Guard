package com.temporedata.guard.modules.police.watch.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.watch.WatchAlertRes;
import com.temporedata.guard.api.watch.WatchListRes;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.police.watch.service.WatchService;
import com.temporedata.guard.modules.security.security.audit.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watch")
@RequiredArgsConstructor
public class WatchController {

    private final WatchService watchService;

    // ---- 布控名单 ----
    @GetMapping("/lists")
    public BaseResponse<List<WatchListRes>> lists(@RequestParam(required = false) String status,
                                                  @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(watchService.listLists(status, user.getTenantId()));
    }

    @Audit(action = "CREATE", targetType = "WATCH_LIST", targetName = "#{#body.get('targetName')}", detail = "布控 #{#body.get('listGroup')} L#{#body.get('level')}")
    @PostMapping("/lists")
    public BaseResponse<WatchListRes> createList(@RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal LoginUser user) {
        LocalDateTime from = body.get("effectiveFrom") != null && !body.get("effectiveFrom").isBlank()
                ? LocalDateTime.parse(body.get("effectiveFrom")) : null;
        LocalDateTime to = body.get("effectiveTo") != null && !body.get("effectiveTo").isBlank()
                ? LocalDateTime.parse(body.get("effectiveTo")) : null;
        return BaseResponse.success(watchService.createList(
                body.get("entityId"), body.get("entityType"), body.get("targetName"),
                body.get("listGroup"),
                body.get("level") != null ? Integer.parseInt(body.get("level")) : null,
                body.get("reason"), body.get("density"), from, to, user.getTenantId()));
    }

    @Audit(action = "UPDATE", targetType = "WATCH_LIST", targetName = "#{#id}", detail = "状态->#{#status}")
    @PutMapping("/lists/{id}/status")
    public BaseResponse<WatchListRes> updateListStatus(@PathVariable String id, @RequestParam String status) {
        return BaseResponse.success(watchService.updateListStatus(id, status));
    }

    // ---- 预警闭环 ----
    @Audit(action = "EXECUTE", targetType = "WATCH_ALERT", targetName = "#{#body.get('targetName')}", detail = "命中 #{#body.get('hitScene')}")
    @PostMapping("/fire")
    public BaseResponse<WatchAlertRes> fire(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(watchService.fireAlert(
                body.get("entityId"), body.get("targetName"), body.get("hitScene"),
                body.get("hitSource"), user.getTenantId()));
    }

    @GetMapping("/alerts")
    public BaseResponse<List<WatchAlertRes>> alerts(@RequestParam(required = false) String status,
                                                    @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(watchService.listAlerts(status, user.getTenantId()));
    }

    @Audit(action = "UPDATE", targetType = "WATCH_ALERT", targetName = "#{#id}", detail = "签收->#{#body.get('assignee')}")
    @PostMapping("/alerts/{id}/assign")
    public BaseResponse<WatchAlertRes> assign(@PathVariable String id, @RequestBody Map<String, String> body,
                                              @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(watchService.assign(id, body.getOrDefault("assignee", user.getUsername()),
                user.getTenantId()));
    }

    @Audit(action = "UPDATE", targetType = "WATCH_ALERT", targetName = "#{#id}", detail = "处置结果=#{#body.get('result')}")
    @PostMapping("/alerts/{id}/process")
    public BaseResponse<WatchAlertRes> process(@PathVariable String id, @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(watchService.process(id, body.get("feedback"), body.get("result"),
                body.get("merit"), user.getTenantId()));
    }

    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(watchService.stats(user.getTenantId()));
    }
}
