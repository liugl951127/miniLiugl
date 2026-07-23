package com.minimax.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.minimax.chat", "com.minimax.chat.memory_ext", "com.minimax.common"
})

@MapperScan({
        "com.minimax.chat.mapper",
        "com.minimax.chat.memory_ext.mapper",
        "com.minimax.chat.memory_ext.longterm",
        "com.minimax.chat.memory_ext.pref"
})
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
