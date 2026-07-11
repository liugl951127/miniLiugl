package com.minimax.ai.generation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话上下文跟踪 (V2.8.4)
 *
 * <h3>能力</h3>
 * <ul>
 *   <li><b>多轮会话</b>: 同一 session 内的用户输入关联分析</li>
 *   <li><b>指代消解</b>: "再画一个" → 复用上次的图表类型</li>
 *   <li><b>上下文继承</b>: "改成红色" → 继承上次的对象</li>
 *   <li><b>实体记忆</b>: 记住用户提到的数据/参数</li>
 * </ul>
 *
 * <h3>示例</h3>
 * <pre>{@code
 *   T1: 给我画个柱状图
 *   T2: 改成红色的        → 继承 T1 的柱状图
 *   T3: 再画个折线的      → 改类型但保留会话
 *   T4: 数据大点          → 调整参数
 * }</pre>
 */
@Slf4j
@Component
public class ConversationContext {

    /** 单 session 最多保留轮数 */
    private static final int MAX_TURNS = 10;

    /** session → 上下文 */
    private final Map<String, SessionContext> contexts = new ConcurrentHashMap<>();

    /**
     * 记录一轮对话
     */
    public void record(String sessionId, String userText, KeywordEngine.Intent intent, Map<String, Object> params) {
        if (sessionId == null) sessionId = "default";
        SessionContext ctx = contexts.computeIfAbsent(sessionId, k -> new SessionContext());
        synchronized (ctx) {
            Turn t = Turn.builder()
                    .timestamp(Instant.now())
                    .userText(userText)
                    .intent(intent)
                    .params(params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>())
                    .build();
            ctx.turns.add(t);
            if (ctx.turns.size() > MAX_TURNS) {
                ctx.turns.remove(0);
            }
            ctx.lastIntent = intent;
            ctx.lastParams = params;
            log.debug("[ctx] session={} turn={} intent={}", sessionId, ctx.turns.size(), intent);
        }
    }

    /**
     * 获取最近 N 轮的意图 (用于上下文关联判断)
     */
    public List<KeywordEngine.Intent> recentIntents(String sessionId, int n) {
        if (sessionId == null) sessionId = "default";
        SessionContext ctx = contexts.get(sessionId);
        if (ctx == null) return Collections.emptyList();
        synchronized (ctx) {
            int size = ctx.turns.size();
            int start = Math.max(0, size - n);
            List<KeywordEngine.Intent> result = new ArrayList<>();
            for (int i = start; i < size; i++) result.add(ctx.turns.get(i).intent);
            return result;
        }
    }

    /**
     * 拿到最近 N 轮的用户原文 (用于上下文合成)
     */
    public List<String> recentTexts(String sessionId, int n) {
        if (sessionId == null) sessionId = "default";
        SessionContext ctx = contexts.get(sessionId);
        if (ctx == null) return Collections.emptyList();
        synchronized (ctx) {
            int size = ctx.turns.size();
            int start = Math.max(0, size - n);
            List<String> result = new ArrayList<>();
            for (int i = start; i < size; i++) result.add(ctx.turns.get(i).userText);
            return result;
        }
    }

    /**
     * 合并上轮参数 + 本轮参数 (本轮覆盖上轮)
     */
    public Map<String, Object> mergeParams(String sessionId, Map<String, Object> current) {
        if (sessionId == null) sessionId = "default";
        SessionContext ctx = contexts.get(sessionId);
        if (ctx == null || ctx.lastParams == null) return current;
        Map<String, Object> merged = new LinkedHashMap<>(ctx.lastParams);
        if (current != null) merged.putAll(current);
        return merged;
    }

    /**
     * 清除某 session 上下文
     */
    public void clear(String sessionId) {
        if (sessionId != null) contexts.remove(sessionId);
    }

    /**
     * 整体清空 (运维接口)
     */
    public void clearAll() {
        contexts.clear();
    }

    /** 当前 session 状态 */
    public Map<String, Object> getSnapshot(String sessionId) {
        if (sessionId == null) sessionId = "default";
        SessionContext ctx = contexts.get(sessionId);
        if (ctx == null) return Map.of("turns", 0);
        synchronized (ctx) {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("turns", ctx.turns.size());
            snap.put("lastIntent", ctx.lastIntent);
            snap.put("lastParams", ctx.lastParams);
            return snap;
        }
    }

    @Data
    private static class SessionContext {
        List<Turn> turns = new ArrayList<>();
        KeywordEngine.Intent lastIntent;
        Map<String, Object> lastParams;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Turn {
        Instant timestamp;
        String userText;
        KeywordEngine.Intent intent;
        Map<String, Object> params;
    }
}
