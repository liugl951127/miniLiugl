package com.minimax.gateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MiniMax 大模型平台 - 统一启动入口
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.minimax.**.mapper")
@SpringBootApplication(scanBasePackages = {"com.minimax"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""

                ====================================================
                  MiniMax Platform started successfully
                  API doc:  http://localhost:8080/doc.html
                  Health:   http://localhost:8080/api/v1/health
                ====================================================
                """);
    }
}
