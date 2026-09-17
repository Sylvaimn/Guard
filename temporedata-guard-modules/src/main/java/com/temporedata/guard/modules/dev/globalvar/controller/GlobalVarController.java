package com.temporedata.guard.modules.dev.globalvar.controller;

import com.temporedata.guard.modules.dev.globalvar.entity.GlobalVarEntity;
import com.temporedata.guard.modules.dev.globalvar.service.GlobalVarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/global-var")
@RequiredArgsConstructor
public class GlobalVarController {

    private final GlobalVarService globalVarService;

    @GetMapping
    public List<GlobalVarEntity> list(@RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return globalVarService.listByTenant(tenantId);
    }

    @GetMapping("/map")
    public Map<String, String> asMap(@RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return globalVarService.asMap(tenantId);
    }

    @PostMapping
    public GlobalVarEntity create(@RequestBody Map<String, String> body) {
        return globalVarService.create(
                body.get("varKey"), body.get("varValue"), body.get("description"),
                body.getOrDefault("tenantId", "DEFAULT"));
    }

    @PutMapping("/{id}")
    public GlobalVarEntity update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return globalVarService.update(id, body.get("varKey"), body.get("varValue"), body.get("description"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        globalVarService.delete(id);
    }
}