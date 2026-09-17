package com.temporedata.guard.modules.dev.dependency.controller;

import com.temporedata.guard.modules.dev.dependency.entity.DependencyEntity;
import com.temporedata.guard.modules.dev.dependency.service.DependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dependency")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    @GetMapping
    public List<DependencyEntity> list(@RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return dependencyService.listByTenant(tenantId);
    }

    @GetMapping("/type/{type}")
    public List<DependencyEntity> listByType(@PathVariable String type,
                                              @RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return dependencyService.listByType(type, tenantId);
    }

    @PostMapping
    public DependencyEntity create(@RequestBody Map<String, String> body) {
        return dependencyService.create(
                body.get("name"), body.get("description"), body.get("type"),
                body.get("config"), body.getOrDefault("tenantId", "DEFAULT"));
    }

    @PutMapping("/{id}")
    public DependencyEntity update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return dependencyService.update(id, body.get("name"), body.get("description"),
                body.get("type"), body.get("config"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        dependencyService.delete(id);
    }
}