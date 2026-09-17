package com.temporedata.guard.modules.platform.settings.controller;

import com.temporedata.guard.modules.platform.settings.entity.SettingsEntity;
import com.temporedata.guard.modules.platform.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public List<SettingsEntity> list() {
        return settingsService.listAll();
    }

    @GetMapping("/group/{groupName}")
    public Map<String, String> getGroup(@PathVariable String groupName) {
        return settingsService.getByGroup(groupName);
    }

    @GetMapping("/{key}")
    public String getValue(@PathVariable String key) {
        return settingsService.getValue(key);
    }

    @PostMapping("/group/{groupName}")
    public void saveGroup(@PathVariable String groupName, @RequestBody Map<String, String> configs) {
        settingsService.saveSettings(groupName, configs);
    }

    @PutMapping("/{key}")
    public SettingsEntity setValue(@PathVariable String key, @RequestBody Map<String, String> body) {
        return settingsService.setValue(key, body.get("value"), body.get("description"));
    }

    @DeleteMapping("/{key}")
    public void delete(@PathVariable String key) {
        settingsService.deleteKey(key);
    }
}