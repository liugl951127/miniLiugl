package com.minimax.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    private final String code;

    public static MessageRole of(String code) {
        if (code == null) return USER;
        for (MessageRole r : values()) {
            if (r.code.equalsIgnoreCase(code)) return r;
        }
        return USER;
    }
}
