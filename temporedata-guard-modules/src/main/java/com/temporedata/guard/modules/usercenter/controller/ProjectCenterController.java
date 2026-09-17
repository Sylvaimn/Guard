package com.temporedata.guard.modules.usercenter.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.usercenter.service.ProjectCenterService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专案中心 API(对应前端 v2.1 专案中心 8 页):
 * 专案 CRUD / 成员 / 对象 / 线索 / 任务 / 报告 / 动态。
 * 权限: project:view(成员) / project:update(ANALYST+), 请求头 X-Project-Id 注入专案上下文。
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectCenterController {

    private final ProjectCenterService projectService;

    /** 我的专案列表 */
    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.list(user));
    }

    /** 创建专案 */
    @PostMapping
    public BaseResponse<Map<String, Object>> create(@RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.create(body, user));
    }

    /** 专案详情 */
    @GetMapping("/{id}")
    public BaseResponse<Map<String, Object>> detail(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.detail(id, user));
    }

    /** 专案对象 */
    @GetMapping("/{id}/objects")
    public BaseResponse<List<Map<String, Object>>> objects(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.objects(id, user));
    }

    @PostMapping("/{id}/objects")
    public BaseResponse<Map<String, Object>> addObject(@PathVariable String id, @RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.addObject(id, body, user));
    }

    /** 专案任务 */
    @GetMapping("/{id}/tasks")
    public BaseResponse<List<Map<String, Object>>> tasks(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.tasks(id, user));
    }

    @PostMapping("/{id}/tasks")
    public BaseResponse<Map<String, Object>> addTask(@PathVariable String id, @RequestBody Map<String, String> body,
                                                     @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.addTask(id, body, user));
    }

    /** 专案报告 */
    @GetMapping("/{id}/reports")
    public BaseResponse<List<Map<String, Object>>> reports(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.reports(id, user));
    }

    /** 专案动态 */
    @GetMapping("/{id}/activity")
    public BaseResponse<List<Map<String, Object>>> activity(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.activity(id, user));
    }

    /** 加入专案(从对象/线索/报告页加入) */
    @PostMapping("/{id}/member")
    public BaseResponse<Map<String, Object>> addMember(@PathVariable String id, @RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(projectService.addMember(id, body, user));
    }
}
