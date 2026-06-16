package com.minimax.chat.vo;

import com.minimax.chat.entity.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionVO {
    private Long id;
    private String title;
    private String model;
    private String systemPrompt;
    private BigDecimal temperature;
    private Integer status;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SessionVO from(ChatSession e) {
        if (e == null) return null;
        return SessionVO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .model(e.getModel())
                .systemPrompt(e.getSystemPrompt())
                .temperature(e.getTemperature())
                .status(e.getStatus())
                .messageCount(e.getMessageCount())
                .lastMessageAt(e.getLastMessageAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
