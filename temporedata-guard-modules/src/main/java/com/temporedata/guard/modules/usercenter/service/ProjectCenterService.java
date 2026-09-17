package com.temporedata.guard.modules.usercenter.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 专案中心服务: 专案生命周期(成员/对象/任务/报告/动态)。
 * 权限: 仅专案成员可访问; 写入需成员身份。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCenterService {

    private final JdbcTemplate jdbcTemplate;

    /** 我的专案(成员 + 本单位) */
    public List<Map<String, Object>> list(LoginUser user) {
        return jdbcTemplate.queryForList(
                "SELECT p.id, p.name, p.level, p.status, p.description, p.leader_id AS leader, " +
                        "(SELECT COUNT(*) FROM temporedata_guard_project_member m WHERE m.project_id = p.id) AS members, " +
                        "(SELECT COUNT(*) FROM temporedata_guard_project_object o WHERE o.project_id = p.id) AS objects, " +
                        "p.create_time AS createTime FROM temporedata_guard_project p " +
                        "WHERE p.tenant_id = ? OR p.id IN (SELECT project_id FROM temporedata_guard_project_member WHERE user_id = ?) " +
                        "ORDER BY p.create_time DESC", user.getTenantId(), user.getUserId());
    }

    public Map<String, Object> create(Map<String, String> body, LoginUser user) {
        String name = body.get("name");
        if (name == null || name.isBlank()) throw new BusinessException("专案名称不能为空");
        String id = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_project (id, name, level, status, description, leader_id, tenant_id, create_time) VALUES (?,?,?,?,?,?,?,NOW())",
                id, name, body.getOrDefault("level", "L3"), "ACTIVE",
                body.getOrDefault("description", ""), user.getUserId(), user.getTenantId());
        jdbcTemplate.update("INSERT INTO temporedata_guard_project_member (id, project_id, user_id, username, role) VALUES (?,?,?,?,?)",
                UUID.randomUUID().toString().replace("-", ""), id, user.getUserId(), user.getUsername(), "LEADER");
        logActivity(id, user, "创建专案", name);
        return Map.of("id", id, "name", name, "status", "ACTIVE");
    }

    public Map<String, Object> detail(String id, LoginUser user) {
        requireMember(id, user);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, level, status, description, leader_id AS leader, create_time AS createTime FROM temporedata_guard_project WHERE id = ?", id);
        if (rows.isEmpty()) throw new BusinessException("专案不存在");
        Map<String, Object> out = new LinkedHashMap<>(rows.get(0));
        out.put("objects", objects(id, user));
        out.put("tasks", tasks(id, user));
        out.put("reports", reports(id, user));
        return out;
    }

    public List<Map<String, Object>> objects(String id, LoginUser user) {
        requireMember(id, user);
        return jdbcTemplate.queryForList(
                "SELECT id, name, entity_type AS type, role, evidence FROM temporedata_guard_project_object WHERE project_id = ?", id);
    }

    public Map<String, Object> addObject(String id, Map<String, String> body, LoginUser user) {
        requireMember(id, user);
        String oid = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_project_object (id, project_id, name, entity_type, role, evidence) VALUES (?,?,?,?,?,?)",
                oid, id, body.getOrDefault("name", "未命名对象"), body.getOrDefault("type", "人员"),
                body.getOrDefault("role", ""), body.getOrDefault("evidence", ""));
        logActivity(id, user, "加入对象", body.get("name"));
        return Map.of("id", oid, "name", body.get("name"));
    }

    public List<Map<String, Object>> tasks(String id, LoginUser user) {
        requireMember(id, user);
        return jdbcTemplate.queryForList(
                "SELECT id, task, assignee, deadline, status, result FROM temporedata_guard_project_task WHERE project_id = ?", id);
    }

    public Map<String, Object> addTask(String id, Map<String, String> body, LoginUser user) {
        requireMember(id, user);
        String tid = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_project_task (id, project_id, task, assignee, deadline, status, result) VALUES (?,?,?,?,?,?,?)",
                tid, id, body.getOrDefault("task", "未命名任务"), body.getOrDefault("assignee", user.getUsername()),
                body.getOrDefault("deadline", ""), body.getOrDefault("status", "待处理"), "");
        logActivity(id, user, "新增任务", body.get("task"));
        return Map.of("id", tid);
    }

    public List<Map<String, Object>> reports(String id, LoginUser user) {
        requireMember(id, user);
        return jdbcTemplate.queryForList(
                "SELECT id, report_name AS title, author, status, create_time AS time FROM temporedata_guard_project_report WHERE project_id = ? ORDER BY create_time DESC", id);
    }

    public List<Map<String, Object>> activity(String id, LoginUser user) {
        requireMember(id, user);
        return jdbcTemplate.queryForList(
                "SELECT username AS `user`, action, target, create_time AS time FROM temporedata_guard_project_activity WHERE project_id = ? ORDER BY create_time DESC LIMIT 100", id);
    }

    public Map<String, Object> addMember(String id, Map<String, String> body, LoginUser user) {
        requireMember(id, user);
        String mid = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO temporedata_guard_project_member (id, project_id, user_id, username, role) VALUES (?,?,?,?,?)",
                mid, id, body.getOrDefault("userId", ""), body.getOrDefault("username", ""),
                body.getOrDefault("role", "ANALYST"));
        return Map.of("id", mid);
    }

    /** 成员校验 */
    private void requireMember(String projectId, LoginUser user) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM temporedata_guard_project_member WHERE project_id = ? AND user_id = ?",
                Integer.class, projectId, user.getUserId());
        Integer leader = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM temporedata_guard_project WHERE id = ? AND leader_id = ?",
                Integer.class, projectId, user.getUserId());
        if ((n == null || n == 0) && (leader == null || leader == 0)) {
            throw new BusinessException("无权访问该专案（非专案成员）");
        }
    }

    private void logActivity(String projectId, LoginUser user, String action, String target) {
        try {
            jdbcTemplate.update("INSERT INTO temporedata_guard_project_activity (id, project_id, username, action, target, create_time) VALUES (?,?,?,?,?,NOW())",
                    UUID.randomUUID().toString().replace("-", ""), projectId, user.getUsername(), action, target);
        } catch (Exception e) {
            log.warn("专案动态记录失败: {}", e.getMessage());
        }
    }
}
