package com.minimax.chat.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天模块本地短期记忆 - 用 ConcurrentHashMap 简化实现。
 * 不引第三方依赖（caffeine 等），进程内缓存。
 *
 * 真实生产应：
 *  1) Redis 共享
 *  2) 调 memory 模块的 HTTP API
 *
 * 当前 MVP：进程级，重启数据丢失（可接受）。
 */
@Slf4j
@Component
public class SessionContextCache {

    private static final int MAX_MESSAGES = 100;

    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    public void append(Long sessionId, String role, String content) {
        if (role == null || content == null) return;
        Map<String, String> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        msg.put("ts", String.valueOf(System.currentTimeMillis()));
        try {
            String jsonStr = json.writeValueAsString(msg);
            String key = key(sessionId);
            synchronized (cache) {
                List<String> list = cache.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
                list.add(jsonStr);
                while (list.size() > MAX_MESSAGES) list.remove(0);
            }
        } catch (Exception e) {
            log.warn("写入会话上下文失败: {}", e.getMessage());
        }
    }

    public List<Map<String, String>> recent(Long sessionId, int limit) {
        if (limit <= 0) limit = 20;
        List<String> list = cache.get(key(sessionId));
        if (list == null) return List.of();
        synchronized (list) {
            int from = Math.max(0, list.size() - limit);
            List<Map<String, String>> out = new ArrayList<>(Math.min(limit, list.size()));
            for (int i = from; i < list.size(); i++) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> m = json.readValue(list.get(i), Map.class);
                    out.add(m);
                } catch (Exception ignore) {}
            }
            return out;
        }
    }

    public void clear(Long sessionId) {
        cache.remove(key(sessionId));
    }

    public long size(Long sessionId) {
        List<String> list = cache.get(key(sessionId));
        return list == null ? 0 : list.size();
    }

    private String key(Long id) { return "chat:sess:" + id; }
}
