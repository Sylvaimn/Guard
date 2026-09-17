package com.temporedata.guard.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 轻舟云 API 网关(Spring Cloud Gateway)
 * 统一入口: 鉴权/限流/路由/灰度; 目标架构下按服务前缀路由到各微服务。
 * 当前单体阶段: 静态路由到 8080/8081 双实例(轮询负载均衡)。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
