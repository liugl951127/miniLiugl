package com.minimax.model.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Web 配置 (V6.8.1+)
 *
 * RestTemplate: 用于 LocalModelController 向本地推理服务器发起 HTTP 请求。
 * 本地模型通常在内网，timeout 设置较长。
 */
@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60)); // 本地推理可能较慢
        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
