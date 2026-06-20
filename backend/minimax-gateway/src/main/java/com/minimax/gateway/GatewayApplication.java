package com.minimax.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MiniMax 大模型平台 - 统一网关入口 (V5.5)
 *
 * Spring Cloud Gateway (响应式 WebFlux):
 *   - 12 微服务统一路由
 *   - 前台/后台分流
 *   - 网关级 JWT 鉴权
 *   - Redis 令牌桶限流
 *   - CORS 统一
 *   - 跨域 (CORS) / 限流 / 灰度
 *
 * 端口: 8080
 *
 * 访问: http://localhost:8080/api/v1/{module}/...
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(
    scanBasePackages = {"com.minimax"},
    exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    })
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""

                ====================================================
                  MiniMax Gateway (Spring Cloud Gateway) started
                  Port: 8080
                  Routes: 12 microservices + admin

                  前台: /api/v1/{module}/**
                  后台: /api/v1/admin/**  (SUPER_ADMIN only)
                  健康: /actuator/health
                ====================================================
                """);
    }
}