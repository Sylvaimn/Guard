package com.temporedata.guard.modules.dev.sync.controller;
import com.temporedata.guard.modules.dev.sync.service.SyncSchedulerService;
import com.temporedata.guard.modules.dev.sync.service.SyncService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.sync.SyncTaskReq;
import com.temporedata.guard.api.sync.SyncTaskRes;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.security.security.audit.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final SyncSchedulerService syncSchedulerService;

    @GetMapping
    public BaseResponse<List<SyncTaskRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(syncService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<SyncTaskRes> detail(@PathVariable String id) {
        return BaseResponse.success(syncService.detail(id));
    }

    @PostMapping
    @Audit(action = "CREATE", targetType = "SYNC", targetName = "#{#req.name}")
    public BaseResponse<SyncTaskRes> create(@RequestBody SyncTaskReq req,
                                             @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(syncService.create(req, user.getTenantId()));
    }

    @Audit(action = "DELETE", targetType = "SYNC", targetName = "#{#id}")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        syncService.delete(id);
        return BaseResponse.success();
    }

    @Audit(action = "EXECUTE", targetType = "SYNC", targetName = "#{#id}", detail = "触发同步")
    @PostMapping("/{id}/execute")
    public BaseResponse<Void> execute(@PathVariable String id) {
        syncService.executeAsync(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/schedule")
    public BaseResponse<Void> schedule(@PathVariable String id, @RequestParam String cron) {
        syncSchedulerService.scheduleTask(id, cron);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/unschedule")
    public BaseResponse<Void> unschedule(@PathVariable String id) {
        syncSchedulerService.removeSchedule(id);
        return BaseResponse.success();
    }
}