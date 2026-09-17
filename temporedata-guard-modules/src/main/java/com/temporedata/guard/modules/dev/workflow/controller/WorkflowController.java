package com.temporedata.guard.modules.dev.workflow.controller;
import com.temporedata.guard.modules.dev.workflow.service.WorkflowScheduler;
import com.temporedata.guard.modules.dev.workflow.service.WorkflowService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.schedule.ScheduleReq;
import com.temporedata.guard.api.workflow.WorkflowExecuteRes;
import com.temporedata.guard.api.workflow.WorkflowReq;
import com.temporedata.guard.api.workflow.WorkflowRes;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowExecuteEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowScheduler workflowScheduler;

    @GetMapping
    public BaseResponse<List<WorkflowRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(workflowService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<WorkflowRes> detail(@PathVariable String id) {
        return BaseResponse.success(workflowService.detail(id));
    }

    @PostMapping
    public BaseResponse<WorkflowRes> create(@RequestBody WorkflowReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(workflowService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<WorkflowRes> update(@PathVariable String id, @RequestBody WorkflowReq req) {
        return BaseResponse.success(workflowService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        workflowService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<WorkflowExecuteRes> execute(@PathVariable String id) {
        WorkflowExecuteEntity exec = workflowService.createExecution(id);
        // Submit to priority queue scheduler with the workflow's priority
        workflowScheduler.submit(exec.getId(), "MEDIUM");
        return BaseResponse.success(workflowService.toExecuteRes(exec));
    }

    @GetMapping("/{id}/executions")
    public BaseResponse<List<WorkflowExecuteRes>> executions(@PathVariable String id) {
        return BaseResponse.success(workflowService.executionHistory(id));
    }

    @PutMapping("/{id}/schedule")
    public BaseResponse<Void> schedule(@PathVariable String id, @RequestBody ScheduleReq req) {
        if (req.getEnabled() != null && req.getEnabled() == 1) {
            workflowService.updateSchedule(id, req.getCronExpression());
        } else {
            workflowService.disableSchedule(id);
        }
        return BaseResponse.success();
    }
}