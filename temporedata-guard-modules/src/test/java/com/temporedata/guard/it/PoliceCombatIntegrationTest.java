package com.temporedata.guard.it;

import com.temporedata.guard.api.entitymaster.EntityMasterRes;
import com.temporedata.guard.api.entitymaster.EntityRegisterReq;
import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.api.watch.WatchAlertRes;
import com.temporedata.guard.api.watch.WatchListRes;
import com.temporedata.guard.modules.police.entitymaster.service.EntityMasterService;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.modules.police.watch.service.WatchService;
import com.temporedata.guard.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 警务实战核心业务集成测试(H2 内存库):
 * 实体归一化 / 密级脱敏 / 布控预警闭环 / 情报检索。
 */
@SpringBootTest(classes = PoliceCombatIntegrationTest.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PoliceCombatIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.temporedata.guard")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.temporedata.guard")
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.temporedata.guard")
    static class TestApp {

        /** 生产环境由 zy-main HttpClientConfig 提供, 测试上下文自行补充 */
        @org.springframework.context.annotation.Bean
        org.springframework.web.client.RestTemplate restTemplate() {
            return new org.springframework.web.client.RestTemplate();
        }

        /** 业务指标(AgentService 等依赖): 测试上下文提供 SimpleMeterRegistry */
        @org.springframework.context.annotation.Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }
    }

    @Autowired
    private EntityMasterService entityMasterService;

    @Autowired
    private WatchService watchService;

    @Autowired
    private IntelligenceService intelligenceService;

    private static final String TENANT = "test-tenant-1";
    private final LoginUser internalUser = new LoginUser("u1", "police", null, TENANT, "INTERNAL", Set.of("ADMIN"), true);
    private final LoginUser secretUser = new LoginUser("u2", "chief", null, TENANT, "SECRET", Set.of("ADMIN"), true);

    @BeforeEach
    void cleanUp() {
        entityMasterService.list(null, null, TENANT, "SECRET").forEach(e -> entityMasterService.delete(e.getId()));
    }

    @Test
    @DisplayName("实体归一化: 同主标识多源注册合并为同一实体, 映射累加")
    void register_normalizes_by_primary_key() {
        EntityRegisterReq r1 = registerReq("PERSON", "440101199001011234", "测试甲", "治安综合", "src_0001");
        EntityMasterRes e1 = entityMasterService.register(r1, TENANT, "INTERNAL");
        EntityMasterRes e2 = entityMasterService.register(registerReq("PERSON", "440101199001011234", "测试甲", "刑侦系统", "src_0002"), TENANT, "INTERNAL");

        assertThat(e1.getId()).isEqualTo(e2.getId());
        assertThat(e2.getMappings()).hasSize(2);
        assertThat(e2.getStatus()).isEqualTo("CANDIDATE");
    }

    @Test
    @DisplayName("实体确认: CANDIDATE -> MASTER")
    void confirm_promotes_to_master() {
        EntityMasterRes e = entityMasterService.register(
                registerReq("PERSON", "440101199001011235", "测试乙", "治安综合", "src_0003"), TENANT, "INTERNAL");
        EntityMasterRes confirmed = entityMasterService.confirm(e.getId(), "INTERNAL");
        assertThat(confirmed.getStatus()).isEqualTo("MASTER");
    }

    @Test
    @DisplayName("密级管控: 低于数据密级的用户看到脱敏主标识, 高密级用户看到明文")
    void density_controls_primary_key_visibility() {
        EntityRegisterReq req = registerReq("PERSON", "440101199001011236", "涉密对象", "刑侦系统", "src_0004");
        req.setDensity("SECRET");
        EntityMasterRes e = entityMasterService.register(req, TENANT, "SECRET");

        String masked = entityMasterService.detail(e.getId(), "INTERNAL").getPrimaryKey();
        String plain = entityMasterService.detail(e.getId(), "SECRET").getPrimaryKey();

        assertThat(masked).isEqualTo("440****1236");
        assertThat(plain).isEqualTo("440101199001011236");
    }

    @Test
    @DisplayName("布控预警闭环: 布控 -> 命中 -> 签收 -> 处置, 战果统计联动")
    void watch_alert_full_cycle() {
        EntityMasterRes e = entityMasterService.register(
                registerReq("PERSON", "440101199001011237", "布控目标", "治安综合", "src_0005"), TENANT, "INTERNAL");

        WatchListRes list = watchService.createList(e.getId(), "PERSON", "布控目标", "部级", 1,
                "测试布控", "SECRET", null, null, TENANT);
        assertThat(list.getStatus()).isEqualTo("ACTIVE");

        WatchAlertRes alert = watchService.fireAlert(e.getId(), "布控目标", "卡口过车", "卡口KJ-TEST", TENANT);
        assertThat(alert).isNotNull();
        assertThat(alert.getStatus()).isEqualTo("PENDING");

        WatchAlertRes assigned = watchService.assign(alert.getId(), "张警官", TENANT);
        assertThat(assigned.getStatus()).isEqualTo("PROCESSING");

        WatchAlertRes closed = watchService.process(alert.getId(), "当场抓获", "CAPTURED", "抓获1人", TENANT);
        assertThat(closed.getStatus()).isEqualTo("CLOSED");

        Map<String, Object> stats = watchService.stats(TENANT);
        assertThat(stats.get("closed")).isEqualTo(1L);
        assertThat(stats.get("captured")).isEqualTo(1L);
    }

    @Test
    @DisplayName("布控命中: 未在布控名单中的实体不触发预警")
    void fire_alert_requires_active_list() {
        EntityMasterRes e = entityMasterService.register(
                registerReq("PERSON", "440101199001011238", "无关人员", "治安综合", "src_0006"), TENANT, "INTERNAL");
        WatchAlertRes alert = watchService.fireAlert(e.getId(), "无关人员", "卡口过车", "卡口KJ-TEST", TENANT);
        assertThat(alert).isNull();
    }

    @Test
    @DisplayName("情报检索: 关键字命中 + 主标识脱敏 + 密级过滤")
    void intelligence_search_masks_and_filters() {
        entityMasterService.register(registerReq("PERSON", "440101199001011239", "情报对象", "治安综合", "src_0007"), TENANT, "INTERNAL");
        EntityRegisterReq secretReq = registerReq("PERSON", "440101199001011240", "绝密对象", "刑侦系统", "src_0008");
        secretReq.setDensity("SECRET");
        entityMasterService.register(secretReq, TENANT, "SECRET");

        // INTERNAL 用户: 密级过滤只可见 INTERNAL 数据
        List<IntelligenceRes> hits = intelligenceService.search("情报", null, internalUser);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getDisplayName()).isEqualTo("情报对象");

        // SECRET 用户: 可见全部数据
        List<IntelligenceRes> secretHits = intelligenceService.search("对象", null, secretUser);
        assertThat(secretHits).hasSize(2);

        // 检索批量结果: 主标识一律脱敏(默认最小化)
        assertThat(hits.get(0).getPrimaryKey()).isEqualTo("440****1239");
        assertThat(secretHits.stream().filter(h -> "绝密对象".equals(h.getDisplayName()))
                .findFirst().orElseThrow().getPrimaryKey()).isEqualTo("440****1240");
    }

    private EntityRegisterReq registerReq(String type, String pk, String name, String source, String recordId) {
        EntityRegisterReq req = new EntityRegisterReq();
        req.setEntityType(type);
        req.setPrimaryKey(pk);
        req.setDisplayName(name);
        req.setSourceSystem(source);
        req.setSourceRecordId(recordId);
        return req;
    }
}
