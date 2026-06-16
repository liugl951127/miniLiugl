package com.minimax.agent.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.minimax.agent", "com.minimax.common"})
@EnableAsync
public class AgentApp {
    public static void main(String[] args) {
        SpringApplication.run(AgentApp.class, args);
    }
}
