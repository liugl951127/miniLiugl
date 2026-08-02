package com.minimax.agent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "com.minimax.agent",
        "com.minimax.common",
        "com.minimax.pipeline",
        "com.minimax.pipeline.function_ext",
        "com.minimax.model"  // V3.5.27+: agent 用 model 调 LLM
})
@MapperScan({
    "com.minimax.agent.mapper",
    "com.minimax.pipeline.function_ext.mapper"
})
@EnableAsync
public class AgentApp {
    public static void main(String[] args) {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(AgentApp.class, args);
    }
}
