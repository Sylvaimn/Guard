package com.temporedata.guard.modules.common.ha.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.common.ha.entity.HaServerEntity;
import com.temporedata.guard.modules.common.ha.service.HaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ha")
@RequiredArgsConstructor
public class HaController {

    private final HaService haService;

    @GetMapping("/servers")
    public BaseResponse<List<HaServerEntity>> getServers() {
        return BaseResponse.success(haService.getAllServers());
    }

    @GetMapping("/servers/active")
    public BaseResponse<List<HaServerEntity>> getActiveServers() {
        return BaseResponse.success(haService.getActiveServers());
    }

    @PostMapping("/recover")
    public BaseResponse<Void> recoverJobs() {
        haService.recoverRunningJobs();
        return BaseResponse.success();
    }
}