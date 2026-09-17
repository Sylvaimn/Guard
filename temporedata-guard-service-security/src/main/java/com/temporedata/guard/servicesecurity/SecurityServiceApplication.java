package com.temporedata.guard.servicesecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 安全合规服务(服务拆分试点):
 * 承载 分类分级/敏感治理/审计/审批/消息/黑名单/变更追溯 等安全域能力。
 * 独立进程(:8090) · 独立 Nacos 服务名 zy-service-security · 网关按前缀路由。
 * 共享 MySQL 主库, 与平台服务数据强一致; 不承载 Quartz 调度(由主应用单飞)。
 */
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
@SpringBootApplication(scanBasePackages = {
        "com.temporedata.guard.modules.security.security",
        "com.temporedata.guard.modules.security.permapproval",
        "com.temporedata.guard.modules.security.message",
        "com.temporedata.guard.modules.security.mydata",
        "com.temporedata.guard.modules.security.sensitive",
        "com.temporedata.guard.modules.security.apilog",
        "com.temporedata.guard.modules.security.behaviorlog",
        "com.temporedata.guard.modules.security.loginlog",
        "com.temporedata.guard.modules.security.changeaudit",
        "com.temporedata.guard.modules.security.blacklist",
        "com.temporedata.guard.modules.security.alarm",
        "com.temporedata.guard.modules.security.datalevel",
        "com.temporedata.guard.modules.platform.user",
        "com.temporedata.guard.modules.platform.tenant",
        "com.temporedata.guard.modules.platform.org",
        "com.temporedata.guard.modules.platform.role",
        "com.temporedata.guard.modules.platform.auth",
        "com.temporedata.guard.modules.common.eventbus",
        "com.temporedata.guard.modules.common.storage",
        "com.temporedata.guard.modules.common.async",
        "com.temporedata.guard.security",
        "com.temporedata.guard.common"
})
public class SecurityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityServiceApplication.class, args);
    }
}
