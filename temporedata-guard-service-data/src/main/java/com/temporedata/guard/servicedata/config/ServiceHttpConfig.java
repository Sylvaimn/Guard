package com.temporedata.guard.servicedata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 拆分服务自包含配置: RestTemplate(主应用同款 HttpClientConfig, 独立服务需自带)。
 */
@Configuration
public class ServiceHttpConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
