package com.temporedata.guard.modules.common.driver.controller;

import com.temporedata.guard.modules.common.driver.entity.DriverEntity;
import com.temporedata.guard.modules.common.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public List<DriverEntity> list(@RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return driverService.listByTenant(tenantId);
    }

    @PostMapping
    public DriverEntity create(@RequestBody Map<String, String> body) {
        return driverService.create(
                body.get("name"), body.get("dbType"), body.get("version"),
                body.get("driverPath"), body.getOrDefault("tenantId", "DEFAULT"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        driverService.delete(id);
    }
}