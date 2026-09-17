package com.temporedata.guard.modules.usercenter.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.usercenter.service.UserCenterService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户端 P0 接口(对应前端 v2.1 用户工作台):
 * 工作台聚合 / 全局搜索 / 受控数据查询 / 对象研判 / 线索中心 / 数据申请。
 * 权限: 基于 LoginUser(roles/dataScopes/securityLevel) 后端二次校验。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserCenterService userCenterService;

    /** 用户工作台聚合 */
    @GetMapping("/user/workbench")
    public BaseResponse<Map<String, Object>> workbench(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.workbench(user));
    }

    /** 全局搜索(人员/案件/线索/数据/API 聚合, 权限+密级过滤) */
    @GetMapping("/search")
    public BaseResponse<Map<String, Object>> search(@RequestParam String keyword,
                                                    @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.search(keyword, user));
    }

    /** 受控查询: 可查询数据+字段选项 */
    @GetMapping("/data/query/options")
    public BaseResponse<List<Map<String, Object>>> queryOptions(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.queryOptions(user));
    }

    /** 受控查询: 禁止任意 SQL, 白名单表/字段 + 条件 + LIMIT */
    @PostMapping("/data/query")
    public BaseResponse<Map<String, Object>> query(@RequestBody Map<String, Object> body,
                                                   @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.query(body, user));
    }

    /** 查询历史(本人) */
    @GetMapping("/data/query/history")
    public BaseResponse<List<Map<String, Object>>> queryHistory(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.queryHistory(user));
    }

    /** 对象研判画像 */
    @GetMapping("/intelligence/entity/{entityId}")
    public BaseResponse<Map<String, Object>> entityProfile(@PathVariable String entityId,
                                                           @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.entityProfile(entityId, user));
    }

    /** 线索中心 */
    @GetMapping("/clue")
    public BaseResponse<List<Map<String, Object>>> clues(@RequestParam(required = false) String level,
                                                         @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.clues(level, user));
    }

    /** 线索登记 */
    @PostMapping("/clue")
    public BaseResponse<Map<String, Object>> createClue(@RequestBody Map<String, String> body,
                                                        @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.createClue(body, user));
    }

    /** 数据申请发起 */
    @PostMapping("/apply")
    public BaseResponse<Map<String, Object>> apply(@RequestBody Map<String, String> body,
                                                   @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.apply(body, user));
    }

    /** 我的申请 */
    @GetMapping("/apply/mine")
    public BaseResponse<List<Map<String, Object>>> myApplies(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.myApplies(user));
    }

    /** 申请审批列表(管理员) */
    @GetMapping("/apply/pending")
    public BaseResponse<List<Map<String, Object>>> pendingApplies(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.pendingApplies(user));
    }

    /** 申请审批: approve / reject */
    @PostMapping("/apply/{id}/approve")
    public BaseResponse<Map<String, Object>> approve(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.decideApply(id, "APPROVED", user));
    }

    @PostMapping("/apply/{id}/reject")
    public BaseResponse<Map<String, Object>> reject(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.decideApply(id, "REJECTED", user));
    }

    // ==================== P2: 角色权限 / 脱敏规则(管理员端配置) ====================

    /** 角色权限列表 */
    @GetMapping("/role/permissions")
    public BaseResponse<List<Map<String, Object>>> rolePermissions(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.rolePermissions(user));
    }

    /** 保存角色权限 */
    @PostMapping("/role/permissions")
    public BaseResponse<Map<String, Object>> saveRolePermissions(@RequestBody Map<String, Object> body,
                                                                 @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.saveRolePermissions(body, user));
    }

    /** 脱敏规则列表 */
    @GetMapping("/security/mask-rules")
    public BaseResponse<List<Map<String, Object>>> maskRules(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.maskRules(user));
    }

    /** 新增脱敏规则 */
    @PostMapping("/security/mask-rules")
    public BaseResponse<Map<String, Object>> addMaskRule(@RequestBody Map<String, String> body,
                                                         @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.addMaskRule(body, user));
    }

    // ==================== P1: 成果中心 / 收藏 / 操作 / API 详情 ====================

    /** 我的报告 */
    @GetMapping("/report/doc")
    public BaseResponse<List<Map<String, Object>>> reports(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.reports(user));
    }

    /** 创建报告 */
    @PostMapping("/report/doc")
    public BaseResponse<Map<String, Object>> createReport(@RequestBody Map<String, String> body,
                                                          @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.createReport(body, user));
    }

    /** 报告详情 */
    @GetMapping("/report/doc/{id}")
    public BaseResponse<Map<String, Object>> reportDetail(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.reportDetail(id, user));
    }

    /** AI 辅助生成 → 生成任务 */
    @PostMapping("/report/doc/{id}/generate")
    public BaseResponse<Map<String, Object>> generateReport(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.generateReport(id, user));
    }

    /** 生成任务列表 */
    @GetMapping("/report/tasks")
    public BaseResponse<List<Map<String, Object>>> reportTasks(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.reportTasks(user));
    }

    /** 我的收藏 */
    @GetMapping("/favorite/mine")
    public BaseResponse<List<Map<String, Object>>> favorites(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.favorites(user));
    }

    /** 添加收藏 */
    @PostMapping("/favorite")
    public BaseResponse<Map<String, Object>> addFavorite(@RequestBody Map<String, String> body,
                                                         @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.addFavorite(body, user));
    }

    /** 我的操作 */
    @GetMapping("/user/ops")
    public BaseResponse<List<Map<String, Object>>> myOps(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.myOps(user));
    }

    /** API 详情(已授权) */
    @GetMapping("/service/api/{id}")
    public BaseResponse<Map<String, Object>> apiDetail(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(userCenterService.apiDetail(id, user));
    }
}
