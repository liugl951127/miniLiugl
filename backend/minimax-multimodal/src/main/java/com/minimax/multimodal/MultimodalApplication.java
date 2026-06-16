package com.minimax.multimodal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.minimax.multimodal",
        "com.minimax.common"
})
public class MultimodalApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultimodalApplication.class, args);
    }
}
