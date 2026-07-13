package com.minimax.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MiniMax AI 独立运行模式 (V2.8.3)
 *
 * <p>不依赖 Nacos / 其他微服务, 直接连 MariaDB + Redis.
 * 适合: 嵌入式部署 / 演示 / 单机 / 边缘计算.</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 *   java -jar minimax-ai.jar --spring.profiles.active=standalone
 *   # 或
 *   SPRING_PROFILES_ACTIVE=standalone java -jar minimax-ai.jar
 * }</pre>
 *
 * <h3>环境变量</h3>
 * <ul>
 *   <li>DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASS</li>
 *   <li>REDIS_HOST / REDIS_PORT / REDIS_PASS</li>
 *   <li>SERVER_PORT (默认 8094)</li>
 * </ul>
 *
 * <h3>已禁用的能力</h3>
 * <ul>
 *   <li>JWT 鉴权 (单服务模式不需要)</li>
 *   <li>Nacos 服务发现 (不注册)</li>
 *   <li>跨服务调用 (如需调 chat/memory, 请用主模式)</li>
 * </ul>
 */
@EnableAsync
@SpringBootApplication
@MapperScan({"com.minimax.ai.mapper", "com.minimax.ai.marketplace", "com.minimax.ai.modelmarket", "com.minimax.ai.template", "com.minimax.ai.webhook"})
public class StandaloneApplication {

    public static void main(String[] args) {
        System.out.println("""
                ╔════════════════════════════════════════╗
                ║   MiniMax AI 独立运行模式 (V2.8.3)        ║
                ║   - 不依赖 Nacos / 其他微服务              ║
                ║   - 端口: 8094                            ║
                ║   - 数据库: ${DB_HOST}:${DB_PORT}         ║
                ║   - Redis: ${REDIS_HOST}:${REDIS_PORT}    ║
                ╚════════════════════════════════════════╝
                """);
        SpringApplication.run(StandaloneApplication.class, args);
    }
}
