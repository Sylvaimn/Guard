package com.temporedata.guard.modules.unit;

import com.temporedata.guard.modules.agent.aianalysis.security.AgentSecurityGuard;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Agent 工具权限守卫单测: 警务工具集/管理工具隔离/角色兜底 */
class AgentSecurityGuardTest {

    @Test
    void policeUserCanUseIntelligenceTools() {
        assertTrue(AgentSecurityGuard.canUseTool("entity_search", Set.of("POLICE_USER"), Set.of("intelligence:view")));
        assertTrue(AgentSecurityGuard.canUseTool("graph_expand", Set.of("POLICE_USER"), Set.of("*:*")));
        assertFalse(AgentSecurityGuard.canUseTool("graph_expand", Set.of("POLICE_USER"), Set.of("intelligence:view")));
    }

    @Test
    void adminToolsBlockedForNormalUser() {
        assertFalse(AgentSecurityGuard.canUseTool("sql_assist", Set.of("POLICE_USER"), Set.of("intelligence:view")));
        assertTrue(AgentSecurityGuard.canUseTool("sql_assist", Set.of("ADMIN"), Set.of()));
        assertTrue(AgentSecurityGuard.canUseTool("ops_diagnose", Set.of("DEVELOPER"), Set.of()));
    }

    @Test
    void permissionsNullFallsBackToRole() {
        assertTrue(AgentSecurityGuard.canUseTool("track_query", Set.of("POLICE_USER"), null));
        assertFalse(AgentSecurityGuard.canUseTool("quality_diagnose", Set.of("POLICE_USER"), null));
    }

    @Test
    void requireToolThrowsWhenDenied() {
        assertThrows(com.temporedata.guard.api.base.exceptions.BusinessException.class,  
                () -> AgentSecurityGuard.requireTool("sql_assist", Set.of("POLICE_USER"), null));
    }
}
