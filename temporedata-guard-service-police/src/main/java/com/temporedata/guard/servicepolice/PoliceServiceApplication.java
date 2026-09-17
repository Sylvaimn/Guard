package com.temporedata.guard.servicepolice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 警务数据服务(服务拆分): 情报检索/实体归一化/实体图谱/轨迹/资金/通联/以图找人/布控预警。
 * 独立进程(:8093) · Nacos 服务名 zy-service-police · 网关按前缀路由。
 * 共享 MySQL 主库; 不承载 Quartz(主应用单飞); Flyway 由主应用负责。
 */
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
@SpringBootApplication(scanBasePackages = {
        "com.temporedata.guard.modules.police.intelligence",
        "com.temporedata.guard.modules.police.entitymaster",
        "com.temporedata.guard.modules.police.entitygraph",
        "com.temporedata.guard.modules.police.network",
        "com.temporedata.guard.modules.police.track",
        "com.temporedata.guard.modules.police.photo",
        "com.temporedata.guard.modules.police.watch",
        "com.temporedata.guard.modules.common.eventbus",
        "com.temporedata.guard.modules.common.storage",
        "com.temporedata.guard.modules.common.async",
        "com.temporedata.guard.security",
        "com.temporedata.guard.common",
        "com.temporedata.guard.servicepolice.config"
})
public class PoliceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoliceServiceApplication.class, args);
    }
}
