package com.minimax.memory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.minimax.memory",
        "com.minimax.common"
})
@MapperScan("com.minimax.memory")
public class MemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }
}
