package com.minimax.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HttpClient Bean 配置 (V3.5.27+)
 *
 * <p>背景: 之前 MonitorController 自己 new HttpClient (局部变量, OK)
 *       DingTalkAlertNotifier 用 @Resource 注入, 但没显式 Bean, 启动会
 *       NoSuchBeanDefinitionException: HttpClient</p>
 *
 * <p>修法: 显式 @Bean HttpClient, 共享连接池, 配置超时</p>
 */
@Configuration
public class HttpClientConfig {

    /**
     * 默认 HttpClient Bean (V3.5.27+)
     *
     * <p>connectTimeout: 5s (跟 MonitorController 局部变量一致)
     * <p>共享给所有需要 HTTP 调用的组件 (DingTalk / Email / 跨服务)
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
}
