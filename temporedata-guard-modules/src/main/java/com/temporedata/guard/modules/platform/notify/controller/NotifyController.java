package com.temporedata.guard.modules.platform.notify.controller;

import com.temporedata.guard.modules.platform.notify.entity.NotifyEntity;
import com.temporedata.guard.modules.platform.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @GetMapping
    public List<NotifyEntity> list(@RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return notifyService.listByTenant(tenantId);
    }

    @PostMapping
    public NotifyEntity create(@RequestBody Map<String, String> body) {
        return notifyService.create(
                body.get("channelType"), body.get("name"),
                body.get("config"), body.getOrDefault("tenantId", "DEFAULT"));
    }

    @PutMapping("/{id}")
    public NotifyEntity update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return notifyService.update(id, body.get("channelType"), body.get("name"), body.get("config"));
    }

    @PutMapping("/{id}/toggle")
    public void toggle(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        notifyService.toggleEnabled(id, body.getOrDefault("enabled", false));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        notifyService.delete(id);
    }
}