package com.minimax.chat.vo;

import com.minimax.chat.entity.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageVO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Integer tokens;
    private String finishReason;
    private LocalDateTime createdAt;

    public static MessageVO from(ChatMessage e) {
        if (e == null) return null;
        return MessageVO.builder()
                .id(e.getId())
                .sessionId(e.getSessionId())
                .role(e.getRole())
                .content(e.getContent())
                .tokens(e.getTokens())
                .finishReason(e.getFinishReason())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
