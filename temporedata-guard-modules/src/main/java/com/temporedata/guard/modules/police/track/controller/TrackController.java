package com.temporedata.guard.modules.police.track.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.security.security.audit.Audit;
import com.temporedata.guard.modules.police.track.entity.TrackPointEntity;
import com.temporedata.guard.modules.police.track.service.TrackService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 轨迹分析 API（P1）。
 */
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /** 录入轨迹点(测试/接入事件) */
    @PostMapping("/points")
    @Audit(action = "CREATE", targetType = "TRACK_POINT", targetName = "#{#{#body?.get('entityId')}}", detail = "录入轨迹点")
    public BaseResponse<Integer> addPoints(@RequestBody Map<String, Object> body,
                                           @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(trackService.addPoints(
                String.valueOf(body.get("entityId")), String.valueOf(body.getOrDefault("entityType", "PERSON")),
                (List<Map<String, Object>>) body.get("points"), user.getTenantId()));
    }

    /** 时段轨迹查询 */
    @GetMapping("/{entityId}")
    public BaseResponse<List<TrackPointEntity>> query(
            @PathVariable String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(trackService.query(entityId, from, to, user.getTenantId()));
    }

    /** 落脚点聚合 */
    @GetMapping("/{entityId}/footprints")
    public BaseResponse<List<Map<String, Object>>> footprints(
            @PathVariable String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(trackService.footprints(entityId, from, to, user.getTenantId()));
    }

    /** 活动规律摘要(Agent 文本化用) */
    @GetMapping("/{entityId}/summary")
    public BaseResponse<Map<String, Object>> summary(
            @PathVariable String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(trackService.summary(entityId, from, to, user.getTenantId()));
    }
}
