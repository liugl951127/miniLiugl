package com.minimax.deployer.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/** HTTP 客户端配置 (V4.0) — 单一 RestTemplate Bean */
@Configuration
public class HttpClientConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(5))
                      .setReadTimeout(Duration.ofSeconds(30))
                      .build();
    }
}
