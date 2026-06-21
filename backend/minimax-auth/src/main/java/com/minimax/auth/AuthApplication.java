package com.minimax.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auth 模块独立启动类。
 * 部署时与 gateway 通过 HTTP 通信；本地调试可单独启动。
 */
@SpringBootApplication(scanBasePackages = {
        "com.minimax.auth",
        "com.minimax.common",
        "com.minimax.common.config"  // 注入 WebSocket 等通用组件
})
@MapperScan("com.minimax.auth.mapper")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
