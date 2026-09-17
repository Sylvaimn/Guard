package com.temporedata.guard.modules.common.resource.controller;

import com.temporedata.guard.modules.common.resource.entity.ResourceEntity;
import com.temporedata.guard.modules.common.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService;

    @GetMapping
    public List<ResourceEntity> list(@RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return resourceService.listByTenant(tenantId);
    }

    @GetMapping("/type/{type}")
    public List<ResourceEntity> listByType(@PathVariable String type,
                                            @RequestParam(defaultValue = "DEFAULT") String tenantId) {
        return resourceService.listByType(type, tenantId);
    }

    @PostMapping
    public ResourceEntity create(@RequestBody Map<String, Object> body) {
        return resourceService.create(
                (String) body.get("name"), (String) body.get("type"),
                (String) body.get("path"), body.get("size") != null ? ((Number) body.get("size")).longValue() : null,
                (String) body.get("description"), (String) body.getOrDefault("tenantId", "DEFAULT"));
    }

    @PutMapping("/{id}")
    public ResourceEntity update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return resourceService.update(id, body.get("name"), body.get("description"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        resourceService.delete(id);
    }

    /** 登记资源血缘: 文件节点 + LOAD(文件→表)/EXPORT(表→文件) 边 */
    @PostMapping("/{id}/lineage/register")
    public Map<String, Object> registerLineage(@PathVariable String id,
                                               @RequestBody Map<String, String> body) {
        ResourceEntity r = resourceService.get(id);
        lineageService.collectResourceLineage(
                r.getTenantId(), r.getId(), r.getName(),
                body.get("tableName"),
                body.getOrDefault("relationType", "LOAD"));
        resourceService.updateLineageRelation(id, body.get("tableName"), body.get("relationType"));
        return Map.of("code", 0, "resourceId", r.getId(),
                "nodeId", "J:" + r.getTenantId() + ":FILE:" + r.getName());
    }

    /** 资源血缘链路查询 */
    @GetMapping("/{id}/lineage")
    public Map<String, Object> lineage(@PathVariable String id) {
        ResourceEntity r = resourceService.get(id);
        String nodeId = "J:" + r.getTenantId() + ":FILE:" + r.getName();
        return Map.of("resourceId", r.getId(), "resourceName", r.getName(),
                "nodeId", nodeId,
                "edges", lineageService.resourceLineage(r.getTenantId(), nodeId));
    }
}