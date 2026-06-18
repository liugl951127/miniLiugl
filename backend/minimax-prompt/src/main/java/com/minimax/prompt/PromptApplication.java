package com.minimax.prompt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Prompt 模板模块启动类 (V4.3 — 8091 端口).
 */
@SpringBootApplication(scanBasePackages = {"com.minimax.prompt", "com.minimax.common"})
@MapperScan("com.minimax.prompt.mapper")
public class PromptApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptApplication.class, args);
    }
}
