package com.temporedata.guard.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 轻舟云 qingzhouyun - application entry point.
 */
@SpringBootApplication
@org.springframework.kafka.annotation.EnableKafka
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = "com.temporedata.guard")
@EnableJpaRepositories(basePackages = "com.temporedata.guard.modules")
@EntityScan(basePackages = "com.temporedata.guard.modules")
public class ZyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZyApplication.class, args);
    }
}