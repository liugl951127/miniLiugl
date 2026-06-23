// =============================================================
// MiniMax 大模型平台 - 统一网关入口 (V5.5)
// =============================================================
//
// 这是整个 Spring Cloud Gateway 应用的启动入口类。
//
// 核心职责 (V5.5+):
//   1. 加载 application.yml 配置 (注册中心 Nacos / Redis 限流)
//   2. 启动 WebFlux 响应式 Web 容器 (Netty)
//   3. 注册 13 条路由规则 (12 微服务 + admin)
//   4. 装配 4 个全局过滤器:
//      - TraceFilter (V5.8): TraceId 注入 + 全链路追踪
//      - JwtAuthGlobalFilter (V5.5): 网关级 JWT 鉴权 (免去业务模块重复)
//      - MetricsFilter: HTTP 请求自动采点 (Micrometer + Prometheus)
//      - 限流 (Bucket4j + Redis): 全局限流
//
// 端口: 8080 (统一入口, 外部访问)
// 内部路由: lb://minimax-{module} (V5.7 Nacos 服务发现)
//
// 启动命令:
//   本地: mvn spring-boot:run -pl minimax-gateway
//   Docker: docker compose up -d gateway
//   访问: http://localhost:8080/api/v1/{module}/...
// =============================================================

// Java 标准包 - 当前类所属包
package com.minimax.gateway;

// Spring Boot 启动器 - run() 方法启动 Spring 容器
import org.springframework.boot.SpringApplication;
// Spring Boot 自动配置注解 - @SpringBootApplication 内部依赖
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 启用 @Async 异步方法支持 (controller 可异步返回 SSE 流)
import org.springframework.scheduling.annotation.EnableAsync;
// 启用 @Scheduled 定时任务支持 (健康检查 / 心跳)
import org.springframework.scheduling.annotation.EnableScheduling;
// Slf4j 日志 (V5.33 Day 19)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gateway 启动类.
 *
 * 启动后默认行为:
 *   1. 扫描 com.minimax.* 包下所有 @Component / @Service / @RestController
 *   2. 排除 JDBC/JPA 自动配置 (gateway 不需要数据库, 只做路由)
 *   3. 排除 Spring Security 自动配置 (gateway 用自己的 JwtAuthGlobalFilter)
 *   4. 加载 application.yml + application-common.yml
 *   5. 从 Nacos 拉取服务列表, 注册路由 lb://
 */
@EnableAsync                       // 启用异步 (@Async 方法会跑在 TaskExecutor 线程池)
@EnableScheduling                  // 启用定时任务 (@Scheduled 方法会按 cron 触发)
@SpringBootApplication(            // 标记这是 Spring Boot 应用入口
    scanBasePackages = {"com.minimax"},  // 扫描 com.minimax 包下所有组件 (含 minimax-common)
    exclude = {                     // 排除以下自动配置 (gateway 用不到, 启动更快)
        // 1. 不需要 DataSource (gateway 不直连数据库)
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        // 2. 不需要 JPA (同上)
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        // 3. 不需要 Spring Security 默认 UserDetailsService (用自实现的 JwtAuthGlobalFilter)
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        // 4. 不需要 Spring Security 过滤器链 (gateway 走 WebFlux, 不是 servlet)
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        // 5. 不需要 Spring Security Filter (同上)
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    })
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    /**
     * JVM 入口方法.
     *
     * 调用 SpringApplication.run() 启动 Spring Boot 容器.
     * 启动后 WebFlux 容器 (默认 Netty) 监听 8080 端口.
     *
     * @param args 命令行参数 (Spring 标准: --server.port=9090 等)
     */
    public static void main(String[] args) {
        // 委托给 SpringApplication 启动 (扫描配置 + 启动容器)
        SpringApplication.run(GatewayApplication.class, args);
        // 打印启动横幅 (运维一眼看到是否启动成功 + 端口 + 路由数)
        log.info("""

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
