package com.temporedata.guard.modules.agent.aianalysis.security;

import com.temporedata.guard.api.base.exceptions.BusinessException;

import java.util.Map;
import java.util.Set;

/**
 * Agent 工具权限守卫(权限继承):
 * 1. 工具最低密级(沿用 AiDensityGuard);
 * 2. 工具所需操作权限(用户端仅警务工具集, 管理工具仅管理员);
 * 3. 校验通过返回 true, 失败抛 400 明确文案。
 */
public final class AgentSecurityGuard {

    /** 工具 → 所需操作权限(与前端 v-permission 编码一致) */
    private static final Map<String, String> TOOL_PERMISSION = Map.of(
            "entity_search", "intelligence:view",
            "graph_expand", "intelligence:graph",
            "track_query", "intelligence:detail",
            "watch_status", "intelligence:view",
            "case_analysis", "intelligence:detail");

    /** 管理工具集(仅 ADMIN/DEVELOPER/AUDITOR/SECURITY 角色可用) */
    private static final Set<String> ADMIN_TOOLS = Set.of(
            "quality_diagnose", "lineage_explain", "sql_assist", "ops_diagnose", "workflow_analyze");

    private AgentSecurityGuard() {
    }

    public static boolean canUseTool(String tool, Set<String> roleCodes, Set<String> permissions) {
        boolean isAdmin = roleCodes != null && roleCodes.stream()
                .anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        if (ADMIN_TOOLS.contains(tool)) {
            return isAdmin;
        }
        // 管理工具之外: 警务工具集(普通用户默认可用)
        if (permissions == null) {
            return true;
        }
        String need = TOOL_PERMISSION.get(tool);
        if (need == null) {
            return true;
        }
        return permissions.contains("*:*") || permissions.contains(need);
    }

    public static void requireTool(String tool, Set<String> roleCodes, Set<String> permissions) {
        if (!canUseTool(tool, roleCodes, permissions)) {
            throw new BusinessException("当前账户无权调用该智能工具: " + tool);
        }
    }
}
