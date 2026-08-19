package com.minimax.agent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication(scanBasePackages = {
        "com.minimax.agent",
        "com.minimax.common",
        "com.minimax.model"  // V3.5.27+: agent 用 model 调 LLM
})
@MapperScan({
    "com.minimax.agent.mapper"
})
@EnableAsync
@EnableFeignClients(clients = {
        com.minimax.agent.feign.PipelineFunctionClient.class,
        com.minimax.agent.feign.SkillApprovalClient.class,
        com.minimax.agent.feign.AuthApiKeyClient.class
})
public class AgentApp {
    public static void main(String[] args) {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(AgentApp.class, args);
    }
}
