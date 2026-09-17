package com.temporedata.guard.modules.common.monitor.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.common.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/system")
    public BaseResponse<Map<String, Object>> getSystemMonitor() {
        return BaseResponse.success(monitorService.getSystemMonitor());
    }

    @GetMapping("/cluster")
    public BaseResponse<List<Map<String, Object>>> getClusterMonitor(
            @RequestParam String clusterId,
            @RequestParam(defaultValue = "1_HOUR") String timeType) {
        return BaseResponse.success(monitorService.getClusterMonitor(clusterId, timeType));
    }

    @GetMapping("/instance")
    public BaseResponse<Map<String, Object>> getInstanceMonitor() {
        return BaseResponse.success(monitorService.getInstanceMonitor());
    }
}