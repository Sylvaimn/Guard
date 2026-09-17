package com.temporedata.guard.servicedev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 数据研发服务(服务拆分): 数据源/工作流/采集/同步/质量/血缘/元数据/目录/引擎作业。
 * 独立进程(:8095) · Nacos 服务名 zy-service-dev · 网关按前缀路由。
 * 共享 MySQL; 不承载 Quartz(主应用单飞); Flyway 由主应用负责。
 */
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
@SpringBootApplication(scanBasePackages = {
        "com.temporedata.guard.modules.dev.workflow",
        "com.temporedata.guard.modules.data.datasource",
        "com.temporedata.guard.modules.dev.meta",
        "com.temporedata.guard.modules.dev.catalog",
        "com.temporedata.guard.modules.dev.quality",
        "com.temporedata.guard.modules.dev.sync",
        "com.temporedata.guard.modules.dev.lineage",
        "com.temporedata.guard.modules.dev.query",
        "com.temporedata.guard.modules.dev.engine",
        "com.temporedata.guard.modules.dev.real",
        "com.temporedata.guard.modules.dev.indicator",
        "com.temporedata.guard.modules.dev.tag",
        "com.temporedata.guard.modules.dev.datacenter",
        "com.temporedata.guard.modules.dev.dashboard",
        "com.temporedata.guard.modules.security.changeaudit",
        "com.temporedata.guard.modules.dev.dependency",
        "com.temporedata.guard.modules.dev.func",
        "com.temporedata.guard.modules.dev.globalvar",
        "com.temporedata.guard.modules.common.lakehouse",
        "com.temporedata.guard.modules.common.eventbus",
        "com.temporedata.guard.modules.common.storage",
        "com.temporedata.guard.modules.common.async",
        "com.temporedata.guard.security",
        "com.temporedata.guard.common",
        "com.temporedata.guard.servicedev.config"
})
public class DevServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevServiceApplication.class, args);
    }
}
