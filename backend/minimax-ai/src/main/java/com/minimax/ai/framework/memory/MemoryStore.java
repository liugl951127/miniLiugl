package com.minimax.ai.framework.memory;

import com.minimax.ai.framework.agent.Agent.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆存储 (V2.8.6)
 *
 * <h3>两层架构</h3>
 * <ul>
 *   <li><b>短期记忆</b>: 当前 session 的对话流 (LRU + 时间窗口)</li>
 *   <li><b>长期记忆</b>: 跨 session 的用户偏好 + 知识 (按 userId 索引)</li>
 * </ul>
 *
 * <h3>真实数据</h3>
 * 默认内存存储, 真实生产可对接 Redis + DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryStore {

    /** sessionId → 短期记忆列表 (按时间) */
    private final Map<String, List<MemoryItem>> shortTerm = new ConcurrentHashMap<>();
    /** userId → 长期记忆列表 */
    private final Map<Long, List<MemoryItem>> longTermByUser = new ConcurrentHashMap<>();
    /** 单 session 短期记忆最大条数 */
    private static final int MAX_SHORT_TERM = 50;

    /**
     * 记录到短期记忆
     */
    public void remember(String sessionId, MemoryItem item) {
        if (sessionId == null || item == null) return;
        item.sessionId = sessionId;
        List<MemoryItem> list = shortTerm.computeIfAbsent(sessionId, k -> new ArrayList<>());
        synchronized (list) {
            list.add(item);
            if (list.size() > MAX_SHORT_TERM) {
                list.remove(0);  // 删最早的
            }
        }
        log.debug("[memory] short-term remember: session={}, type={}, content='{}'",
                sessionId, item.type, item.content != null && item.content.length() > 30
                        ? item.content.substring(0, 30) + "..." : item.content);
    }

    /**
     * 记录到长期记忆 (跨 session)
     */
    public void rememberLongTerm(Long userId, MemoryItem item) {
        if (userId == null || item == null) return;
        item.userId = userId;
        List<MemoryItem> list = longTermByUser.computeIfAbsent(userId, k -> new ArrayList<>());
        synchronized (list) {
            // 偏好去重
            if (item.type == MemoryItem.Type.USER_PREFERENCE) {
                list.removeIf(x -> x.type == MemoryItem.Type.USER_PREFERENCE
                        && x.content != null
                        && item.content != null
                        && x.content.split("=")[0].equals(item.content.split("=")[0]));
            }
            list.add(item);
        }
        log.info("[memory] long-term remember: user={}, type={}, content='{}'",
                userId, item.type, item.content);
    }

    /**
     * 读取短期记忆
     */
    public List<MemoryItem> recallShortTerm(String sessionId, int maxItems) {
        if (sessionId == null) return Collections.emptyList();
        List<MemoryItem> list = shortTerm.get(sessionId);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            int n = Math.min(maxItems, list.size());
            return new ArrayList<>(list.subList(Math.max(0, list.size() - n), list.size()));
        }
    }

    /**
     * 读取长期记忆 (按类型)
     */
    public List<MemoryItem> recallLongTerm(Long userId, MemoryItem.Type type, int maxItems) {
        if (userId == null) return Collections.emptyList();
        List<MemoryItem> list = longTermByUser.get(userId);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return list.stream()
                    .filter(x -> type == null || x.type == type)
                    .limit(maxItems)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 提取上下文: 用于 Agent prompt 拼装
     */
    public String buildContext(AgentContext ctx, int maxItems) {
        StringBuilder sb = new StringBuilder();
        // 短期
        List<MemoryItem> short_recall = recallShortTerm(ctx.sessionId, maxItems);
        if (!short_recall.isEmpty()) {
            sb.append("[最近对话]\n");
            for (MemoryItem m : short_recall) {
                String role = m.type == MemoryItem.Type.USER_MESSAGE ? "用户" : "助手";
                sb.append(role).append(": ").append(m.content).append("\n");
            }
        }
        // 长期
        if (ctx.userId != null) {
            List<MemoryItem> long_recall = recallLongTerm(ctx.userId, MemoryItem.Type.USER_PREFERENCE, 20);
            if (!long_recall.isEmpty()) {
                sb.append("\n[用户偏好]\n");
                for (MemoryItem m : long_recall) {
                    sb.append("- ").append(m.content).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 清除 session 短期记忆
     */
    public void clearShortTerm(String sessionId) {
        shortTerm.remove(sessionId);
    }

    /**
     * 清除用户所有记忆
     */
    public void clearAll(Long userId) {
        if (userId != null) longTermByUser.remove(userId);
    }

    /** 统计 */
    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("shortTermSessions", shortTerm.size());
        s.put("longTermUsers", longTermByUser.size());
        s.put("totalShortTermItems", shortTerm.values().stream().mapToInt(List::size).sum());
        s.put("totalLongTermItems", longTermByUser.values().stream().mapToInt(List::size).sum());
        return s;
    }
}
