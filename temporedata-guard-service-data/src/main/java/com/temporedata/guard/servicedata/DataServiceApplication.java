package com.temporedata.guard.servicedata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 数据服务(服务拆分): 警务数据接口/研判报表/采集表单/数据源管理。
 * 独立进程(:8092) · Nacos 服务名 zy-service-data · 网关按前缀路由。
 * 共享 MySQL 主库; 不承载 Quartz(主应用单飞); Flyway 由主应用负责。
 */
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
@SpringBootApplication(scanBasePackages = {
        "com.temporedata.guard.modules.data.service",
        "com.temporedata.guard.modules.data.report",
        "com.temporedata.guard.modules.data.form",
        "com.temporedata.guard.modules.data.datasource",
        "com.temporedata.guard.modules.common.eventbus",
        "com.temporedata.guard.modules.common.storage",
        "com.temporedata.guard.modules.common.async",
        "com.temporedata.guard.security",
        "com.temporedata.guard.common",
        "com.temporedata.guard.servicedata.config"
})
public class DataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataServiceApplication.class, args);
    }
}
