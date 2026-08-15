package com.minimax.ai.framework.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 记忆条目 (V2.8.6)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryItem {

    public enum Type {
        USER_MESSAGE,    // 用户消息 (短期)
        AGENT_MESSAGE,   // Agent 回复 (短期)
        USER_PREFERENCE, // 用户偏好 (长期, 跨会话)
        FACT             // 知识事实 (长期)
    }

    public String id;
    public Type type;
    public String sessionId;
    public Long userId;
    public String content;
    public Map<String, Object> metadata;
    public Instant createdAt;

    public static MemoryItem userMessage(String text) {
        return new MemoryItem(null, Type.USER_MESSAGE, null, null, text, null, Instant.now());
    }
    public static MemoryItem agentMessage(String text) {
        return new MemoryItem(null, Type.AGENT_MESSAGE, null, null, text, null, Instant.now());
    }
    public static MemoryItem preference(String key, Object value) {
        return new MemoryItem(null, Type.USER_PREFERENCE, null, null,
                key + "=" + value, Map.of("key", key, "value", value), Instant.now());
    }
}
