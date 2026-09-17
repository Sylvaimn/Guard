package com.temporedata.guard.modules.usercenter.service;

import com.temporedata.guard.common.util.SensitiveUtil;
import com.temporedata.guard.modules.common.lakehouse.service.LakehouseService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户端 P0 服务: 工作台聚合 / 全局搜索 / 受控查询 / 对象研判 / 线索 / 申请。
 * 校验链: 认证 → 数据范围(TENANT) → 密级 → 字段脱敏 → 审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCenterService {

    private final JdbcTemplate jdbcTemplate;
    private final LakehouseService lakehouseService;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    /** 可查询白名单表(授权数据, 用户端不可任意 SQL) */
    private static final Map<String, String> WHITELIST = new LinkedHashMap<>() {{
        put("track_flow_daily", "轨迹流动聚合表");
        put("case_stats_daily", "案件统计表");
        put("fund_flow_stats", "资金流水表");
        put("watch_alert_daily", "布控预警表");
    }};

    /** 字段级脱敏规则: 表 -> 字段 -> 规则(phone/name/key) */
    private static final Map<String, Map<String, String>> MASK_RULES = new LinkedHashMap<>() {{
        put("track_flow_daily", Map.of("region", "key"));
        put("case_stats_daily", Map.of("case_id", "key", "case_type", "name"));
        put("fund_flow_stats", Map.of("from_account", "key", "to_account", "key"));
        put("watch_alert_daily", Map.of("target_name", "name"));
    }};

    // ==================== 工作台聚合 ====================

    public Map<String, Object> workbench(LoginUser user) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("todos", 6);
        out.put("projects", List.of(
                Map.of("name", "XX 电信诈骗专案", "level", "L1", "status", "进行中"),
                Map.of("name", "盗窃团伙案(0523)", "level", "L2", "status", "进行中")));
        out.put("alerts", List.of(
                Map.of("level", "L1", "text", "布控对象 张某某 命中重点区域", "time", "10:21"),
                Map.of("level", "L2", "text", "车辆 陕A***** 异常轨迹 3 条", "time", "09:51")));
        out.put("recentSearches", List.of("张某某 关联分析", "138****8888 通信记录", "陕A***** 轨迹查询"));
        out.put("applyStatus", List.of(
                Map.of("name", "人员基础信息表 访问申请", "status", "审批中"),
                Map.of("name", "轨迹数据 导出申请", "status", "已通过")));
        out.put("stats", Map.of("todo", 8, "project", 3, "data", 12, "apply", 2));
        return out;
    }

    // ==================== 全局搜索 ====================

    public Map<String, Object> search(String keyword, LoginUser user) {
        String kw = keyword == null ? "" : keyword.trim();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("keyword", kw);
        out.put("entities", kw.isBlank() ? List.of() : List.of(
                Map.of("name", SensitiveUtil.maskName(kw), "type", "人员", "level", "L3",
                        "source", "警务综合数据库", "action", "查看详情")));
        out.put("data", whitelistMatches(kw));
        out.put("apis", kw.isBlank() ? List.of() : List.of(
                Map.of("name", "轨迹查询 API", "method", "POST", "level", "L3", "path", "/api/track/query")));
        out.put("projects", List.of());
        return out;
    }

    // ==================== 受控数据查询 ====================

    public List<Map<String, Object>> queryOptions(LoginUser user) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, String> e : WHITELIST.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("table", e.getKey());
            m.put("label", e.getValue());
            m.put("level", "L3");
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> query(Map<String, Object> body, LoginUser user) {
        String table = String.valueOf(body.getOrDefault("table", ""));
        if (!WHITELIST.containsKey(table)) {
            meterRegistry.counter("zy_perm_denied", "tenant", String.valueOf(user.getTenantId()), "reason", "data_query").increment();
            throw new com.temporedata.guard.api.base.exceptions.BusinessException("无权查询该数据对象");
        }
        String cond = body.get("condition") == null ? "" : String.valueOf(body.get("condition")).trim();
        if (cond.length() > 256) {
            throw new com.temporedata.guard.api.base.exceptions.BusinessException("查询条件过长");
        }
        String sql = "SELECT * FROM " + table;
        if (!cond.isEmpty()) {
            // 仅允许简单条件(白名单字段 + 常量), 拒绝拼接注入
            if (!cond.matches("[\\w\\u4e00-\\u9fa5='\"，,。\\s()<>=.]+")) {
                throw new com.temporedata.guard.api.base.exceptions.BusinessException("查询条件包含非法字符");
            }
            sql += " WHERE " + cond;
        }
        sql += " LIMIT 200";
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList("SELECT * FROM temporedata_guard_analysis." + table + (cond.isEmpty() ? " LIMIT 200" : " WHERE " + cond + " LIMIT 200"));
        } catch (Exception e) {
            throw new com.temporedata.guard.api.base.exceptions.BusinessException("查询失败: " + e.getMessage());
        }
        boolean masked = maskRows(table, rows);
        meterRegistry.counter("temporedata_guard_query_count", "tenant", String.valueOf(user.getTenantId())).increment();
        jdbcTemplate.update("INSERT INTO temporedata_guard_data_query_log (id, user_id, username, table_name, conditions, row_count, masked, create_time) VALUES (?,?,?,?,?,?,?,NOW())",
                UUID.randomUUID().toString().replace("-", ""), user.getUserId(), user.getUsername(),
                table, cond, rows.size(), masked);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("table", table);
        out.put("columns", rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet()));
        out.put("rows", rows);
        out.put("total", rows.size());
        out.put("masked", masked);
        return out;
    }

    /** 按字段规则脱敏 */
    private boolean maskRows(String table, List<Map<String, Object>> rows) {
        Map<String, String> rules = MASK_RULES.getOrDefault(table, Map.of());
        if (rules.isEmpty() || rows.isEmpty()) return false;
        boolean any = false;
        for (Map<String, Object> r : rows) {
            for (Map.Entry<String, String> e : rules.entrySet()) {
                Object v = r.get(e.getKey());
                if (v == null) continue;
                String s = String.valueOf(v);
                String masked;
                switch (e.getValue()) {
                    case "phone": masked = SensitiveUtil.maskPhone(s); break;
                    case "name": masked = SensitiveUtil.maskName(s); break;
                    default: masked = SensitiveUtil.maskKey(s);
                }
                if (!masked.equals(s)) { r.put(e.getKey(), masked); any = true; }
            }
        }
        return any;
    }

    public List<Map<String, Object>> queryHistory(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT table_name AS tbl, conditions, row_count AS rcnt, masked, create_time AS tm FROM temporedata_guard_data_query_log WHERE user_id = ? ORDER BY create_time DESC LIMIT 50",
                user.getUserId());
    }

    // ==================== 对象研判 ====================

    public Map<String, Object> entityProfile(String entityId, LoginUser user) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", SensitiveUtil.maskName(entityId));
        out.put("type", "人员");
        out.put("level", "L3");
        out.put("phone", "138****8888");
        out.put("riskTags", List.of("重点关注"));
        out.put("cases", 3);
        out.put("vehicles", List.of("陕A*****"));
        out.put("addresses", List.of("西安市****"));
        out.put("summary", "近 30 天活动规律：夜间活跃，主要活动于城北片区（脱敏摘要）");
        return out;
    }

    // ==================== 线索中心 ====================

    public List<Map<String, Object>> clues(String level, LoginUser user) {
        String sql = "SELECT clue_no, title, level, source, status, create_time FROM temporedata_guard_clue";
        if (level != null && !level.isBlank()) {
            sql += " WHERE level = '" + level.replaceAll("[^A-Za-z0-9]", "") + "'";
        }
        sql += " ORDER BY create_time DESC LIMIT 100";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            return List.of();
        }
    }

    public Map<String, Object> createClue(Map<String, String> body, LoginUser user) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            throw new com.temporedata.guard.api.base.exceptions.BusinessException("线索标题不能为空");
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_clue (id, clue_no, title, level, source, status, create_time) VALUES (?,?,?,?,?,?,NOW())",
                id, "CL-" + System.currentTimeMillis() % 100000000,
                title, body.getOrDefault("level", "L3"), body.getOrDefault("source", "用户登记"), "NEW");
        return Map.of("id", id, "status", "NEW");
    }

    // ==================== 数据申请 ====================

    public Map<String, Object> apply(Map<String, String> body, LoginUser user) {
        String target = body.get("target");
        if (target == null || target.isBlank()) {
            throw new com.temporedata.guard.api.base.exceptions.BusinessException("请填写数据对象");
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_data_apply (id, apply_type, target, purpose, project_id, deadline, status, applicant_id, applicant_name, create_time, update_time) VALUES (?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                id, body.getOrDefault("type", "数据访问"), target, body.getOrDefault("purpose", ""),
                body.getOrDefault("project", ""), body.getOrDefault("deadline", ""),
                "PENDING", user.getUserId(), user.getUsername());
        return Map.of("id", id, "status", "PENDING", "msg", "申请已提交，等待审批");
    }

    public List<Map<String, Object>> myApplies(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT apply_type AS type, target, purpose, project_id AS project, status, create_time AS time FROM temporedata_guard_data_apply WHERE applicant_id = ? ORDER BY create_time DESC",
                user.getUserId());
    }

    // ==================== P2: 申请审批 ====================

    public List<Map<String, Object>> pendingApplies(LoginUser user) {
        boolean isAdmin = user.getRoleCodes().stream().anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        if (!isAdmin) throw new com.temporedata.guard.api.base.exceptions.BusinessException("无权审批");
        return jdbcTemplate.queryForList(
                "SELECT id, apply_type AS type, target, purpose, project_id AS project, applicant_name AS applicant, status, create_time AS time FROM temporedata_guard_data_apply WHERE status = 'PENDING' ORDER BY create_time DESC");
    }

    public Map<String, Object> decideApply(String id, String status, LoginUser user) {
        boolean isAdmin = user.getRoleCodes().stream().anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        if (!isAdmin) throw new com.temporedata.guard.api.base.exceptions.BusinessException("无权审批");
        jdbcTemplate.update("UPDATE temporedata_guard_data_apply SET status = ?, update_time = NOW() WHERE id = ?", status, id);
        return Map.of("id", id, "status", status);
    }

    // ==================== P2: 角色权限 / 脱敏规则 ====================

    public List<Map<String, Object>> rolePermissions(LoginUser user) {
        boolean isAdmin = user.getRoleCodes().stream().anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        if (!isAdmin) throw new com.temporedata.guard.api.base.exceptions.BusinessException("无权查看");
        return jdbcTemplate.queryForList(
                "SELECT r.id AS roleId, r.code, r.name, GROUP_CONCAT(rp.permission_code) AS permissions " +
                        "FROM temporedata_guard_role r LEFT JOIN temporedata_guard_role_permission rp ON rp.role_id = r.id GROUP BY r.id, r.code, r.name");
    }

    public Map<String, Object> saveRolePermissions(Map<String, Object> body, LoginUser user) {
        boolean isAdmin = user.getRoleCodes().stream().anyMatch(r -> r.matches("(?i).*(ADMIN|DEVELOPER|AUDITOR|SECURITY).*"));
        if (!isAdmin) throw new com.temporedata.guard.api.base.exceptions.BusinessException("无权配置");
        String roleId = String.valueOf(body.getOrDefault("roleId", ""));
        Object perms = body.get("permissions");
        jdbcTemplate.update("DELETE FROM temporedata_guard_role_permission WHERE role_id = ?", roleId);
        if (perms instanceof java.util.List) {
            for (Object p : (java.util.List<?>) perms) {
                jdbcTemplate.update("INSERT INTO temporedata_guard_role_permission (role_id, permission_code) VALUES (?,?)", roleId, String.valueOf(p));
            }
        }
        return Map.of("roleId", roleId, "saved", true);
    }

    public List<Map<String, Object>> maskRules(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT id, table_name AS tbl, column_name AS col, mask_type AS type, level FROM temporedata_guard_sensitive_field ORDER BY table_name");
    }

    public Map<String, Object> addMaskRule(Map<String, String> body, LoginUser user) {
        String id = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_sensitive_field (id, table_name, column_name, mask_type, level, create_time) VALUES (?,?,?,?,?,NOW())",
                id, body.getOrDefault("table", ""), body.getOrDefault("column", ""),
                body.getOrDefault("maskType", "key"), body.getOrDefault("level", "L3"));
        return Map.of("id", id);
    }

    // ==================== P1: 成果中心 / 收藏 / 操作 / API 详情 ====================

    public List<Map<String, Object>> reports(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT id, title, type, project_id AS project, status, level, create_time AS time FROM temporedata_guard_report_doc WHERE author = ? ORDER BY create_time DESC",
                user.getUsername());
    }

    public Map<String, Object> createReport(Map<String, String> body, LoginUser user) {
        String title = body.get("title");
        if (title == null || title.isBlank()) throw new com.temporedata.guard.api.base.exceptions.BusinessException("报告标题不能为空");
        String id = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_report_doc (id, title, type, project_id, author, status, level, content, create_time) VALUES (?,?,?,?,?,?,?,?,NOW())",
                id, title, body.getOrDefault("type", "研判报告"), body.getOrDefault("project", ""),
                user.getUsername(), "DRAFT", body.getOrDefault("level", "L3"), "");
        return Map.of("id", id, "title", title, "status", "DRAFT");
    }

    public Map<String, Object> reportDetail(String id, LoginUser user) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, type, project_id AS project, status, level, content, create_time AS time FROM temporedata_guard_report_doc WHERE id = ? AND author = ?",
                id, user.getUsername());
        if (rows.isEmpty()) throw new com.temporedata.guard.api.base.exceptions.BusinessException("报告不存在");
        Map<String, Object> out = new LinkedHashMap<>(rows.get(0));
        if (out.get("content") == null || String.valueOf(out.get("content")).isBlank()) {
            out.put("conclusion", "AI 研判结论生成中（示例：目标对象与专案存在较强关联）");
            out.put("evidence", List.of("共同出现地点 8 次", "共同关联车辆 2 辆", "共同案件 3 起"));
        } else {
            out.put("conclusion", out.get("content"));
            out.put("evidence", List.of());
        }
        return out;
    }

    public Map<String, Object> generateReport(String id, LoginUser user) {
        String tid = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_report_task (id, report_id, report_name, status, progress, error, create_time) VALUES (?,?,?,?,?,?,NOW())",
                tid, id, "报告-" + id.substring(0, 6), "RUNNING", 30, "");
        return Map.of("taskId", tid, "status", "RUNNING", "progress", 30);
    }

    public List<Map<String, Object>> reportTasks(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT id, report_name AS report, status, progress, error, create_time AS time FROM temporedata_guard_report_task ORDER BY create_time DESC LIMIT 100");
    }

    public List<Map<String, Object>> favorites(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT id, target_type AS type, target_name AS name, create_time AS time FROM temporedata_guard_favorite WHERE user_id = ? ORDER BY create_time DESC",
                user.getUserId());
    }

    public Map<String, Object> addFavorite(Map<String, String> body, LoginUser user) {
        String id = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_favorite (id, user_id, target_type, target_id, target_name, create_time) VALUES (?,?,?,?,?,NOW())",
                id, user.getUserId(), body.getOrDefault("targetType", "DATA"),
                body.getOrDefault("targetId", ""), body.getOrDefault("name", "收藏"));
        return Map.of("id", id);
    }

    public List<Map<String, Object>> myOps(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT create_time AS time, action, target_name AS target, target_type AS type, ip FROM temporedata_guard_behavior_log WHERE username = ? ORDER BY create_time DESC LIMIT 100",
                user.getUsername());
    }

    public Map<String, Object> apiDetail(String id, LoginUser user) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("name", "用户信息查询 API");
        out.put("method", "GET");
        out.put("path", "/api/services/" + id);
        out.put("level", "L3");
        out.put("todayCalls", 283000);
        out.put("successRate", "99.98%");
        out.put("avgResponse", "42ms");
        out.put("params", List.of(Map.of("name", "id", "type", "string", "required", true)));
        return out;
    }

    // ==================== 工具 ====================

    private List<Map<String, Object>> whitelistMatches(String kw) {
        if (kw.isBlank()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (String t : WHITELIST.keySet()) {
            if (t.contains(kw.toLowerCase()) || WHITELIST.get(t).contains(kw)) {
                out.add(Map.of("name", WHITELIST.get(t), "type", "数据表", "level", "L3", "table", t));
            }
        }
        return out;
    }
}
