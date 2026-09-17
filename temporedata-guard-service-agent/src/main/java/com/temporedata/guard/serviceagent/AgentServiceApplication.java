package com.temporedata.guard.serviceagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Agent 服务(服务拆分): 意图编排/工具调用/模型网关/RAG/记忆/语义缓存/任务队列。
 * 独立进程(:8094) · Nacos 服务名 zy-service-agent · 网关按前缀路由。
 * 依赖警务域服务(情报/实体/图谱/布控)与查询域服务; 共享 MySQL; 不承载 Quartz。
 */
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
@SpringBootApplication(scanBasePackages = {
        "com.temporedata.guard.modules.agent.agent",
        "com.temporedata.guard.modules.agent.aianalysis",
        "com.temporedata.guard.modules.police.intelligence",
        "com.temporedata.guard.modules.police.entitymaster",
        "com.temporedata.guard.modules.police.entitygraph",
        "com.temporedata.guard.modules.police.watch",
        "com.temporedata.guard.modules.police.track",
        "com.temporedata.guard.modules.police.network",
        "com.temporedata.guard.modules.dev.query",
        "com.temporedata.guard.modules.data.datasource",
        "com.temporedata.guard.modules.common.eventbus",
        "com.temporedata.guard.modules.common.storage",
        "com.temporedata.guard.modules.common.async",
        "com.temporedata.guard.security",
        "com.temporedata.guard.common",
        "com.temporedata.guard.serviceagent.config"
})
public class AgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }
}
