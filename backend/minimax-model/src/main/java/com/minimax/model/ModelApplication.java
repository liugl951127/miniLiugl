package com.minimax.model;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(scanBasePackages = {
        "com.minimax.model", "com.minimax.model.prompt", "com.minimax.common"
})
@MapperScan({"com.minimax.model.mapper", "com.minimax.model.prompt.mapper"})
public class ModelApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModelApplication.class, args);
    }
}
