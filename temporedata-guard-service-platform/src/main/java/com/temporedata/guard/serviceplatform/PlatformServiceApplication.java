package com.temporedata.guard.serviceplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 平台服务(服务拆分): 承载 用户/租户/组织/角色/认证权限 等平台域能力。
 * 独立进程(:8091) · Nacos 服务名 zy-service-platform · 网关按前缀路由。
 * 共享 MySQL; 审计事件消费(Kafka); 湖仓/血缘/CDC 由主应用承载。
 */
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
@SpringBootApplication(scanBasePackages = {
        "com.temporedata.guard.modules.platform.user",
        "com.temporedata.guard.modules.platform.tenant",
        "com.temporedata.guard.modules.platform.org",
        "com.temporedata.guard.modules.platform.role",
        "com.temporedata.guard.modules.platform.auth",
        "com.temporedata.guard.modules.common.eventbus",
        "com.temporedata.guard.modules.common.storage",
        "com.temporedata.guard.modules.common.async",
        "com.temporedata.guard.security",
        "com.temporedata.guard.common",
        "com.temporedata.guard.serviceplatform.config"
})
public class PlatformServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }
}
