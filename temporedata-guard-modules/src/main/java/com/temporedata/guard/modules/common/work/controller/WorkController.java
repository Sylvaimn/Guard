package com.temporedata.guard.modules.common.work.controller;
import com.temporedata.guard.modules.common.work.entity.WorkEntity;
import com.temporedata.guard.modules.common.work.entity.WorkInstanceEntity;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.common.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    @PostMapping("/add")
    public BaseResponse<WorkEntity> addWork(
            @RequestParam String name,
            @RequestParam String workType,
            @RequestParam(required = false) String workflowId,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(workService.addWork(name, workType, workflowId, user.getTenantId(), user.getUsername()));
    }

    @PostMapping("/update")
    public BaseResponse<WorkEntity> updateWork(
            @RequestParam String workId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String config) {
        return BaseResponse.success(workService.updateWork(workId, name, remark, config));
    }

    @PostMapping("/delete")
    public BaseResponse<Void> deleteWork(@RequestParam String workId) {
        workService.deleteWork(workId);
        return BaseResponse.success();
    }

    @PostMapping("/run")
    public BaseResponse<WorkInstanceEntity> runWork(@RequestParam String workId) {
        return BaseResponse.success(workService.runWork(workId));
    }

    @PostMapping("/stop")
    public BaseResponse<Void> stopWork(@RequestParam String workId) {
        workService.stopWork(workId);
        return BaseResponse.success();
    }

    @GetMapping("/{workId}")
    public BaseResponse<WorkEntity> getWork(@PathVariable String workId) {
        return BaseResponse.success(workService.getWork(workId));
    }

    @GetMapping("/page")
    public BaseResponse<Page<WorkEntity>> pageWork(
            @RequestParam(required = false) String workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return BaseResponse.success(workService.pageWork(workflowId, page, pageSize));
    }

    @GetMapping("/list")
    public BaseResponse<List<WorkEntity>> listByWorkflow(@RequestParam String workflowId) {
        return BaseResponse.success(workService.listByWorkflow(workflowId));
    }

    @PostMapping("/copy")
    public BaseResponse<WorkEntity> copyWork(@RequestParam String workId) {
        return BaseResponse.success(workService.copyWork(workId));
    }

    @PostMapping("/top")
    public BaseResponse<Void> topWork(@RequestParam String workId) {
        workService.topWork(workId);
        return BaseResponse.success();
    }

    @GetMapping("/status")
    public BaseResponse<Map<String, Object>> getStatus(@RequestParam String workId) {
        return BaseResponse.success(workService.getStatus(workId));
    }

    @GetMapping("/instances")
    public BaseResponse<List<WorkInstanceEntity>> queryInstance(@RequestParam String workId) {
        return BaseResponse.success(workService.queryInstance(workId));
    }
}