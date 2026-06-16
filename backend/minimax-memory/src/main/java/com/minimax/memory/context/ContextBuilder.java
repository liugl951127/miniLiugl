package com.minimax.memory.context;

import com.minimax.memory.shortterm.ShortTermMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文构建器：按模型 maxContext 智能裁剪消息。
 *
 * 策略（首版 - Day 6）：
 *  1. system prompt 永远保留
 *  2. 最后 1 条 user 消息永远保留（当前问题）
 *  3. 中间消息按时间倒序填充，直到 token 接近 maxContext
 *  4. token 估算：中文/英文混合 1 字符 ≈ 0.6 token
 *
 * 后续 Day 7 会升级为：基于 tiktoken 精确计数 + 摘要压缩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextBuilder {

    private final ShortTermMemory memory;

    /**
     * 构建给模型的 messages 数组。
     * @param sessionId    会话 ID
     * @param systemPrompt 系统提示词（可为 null）
     * @param maxContext   模型最大上下文 token 数
     * @return messages: [{role, content}, ...]
     */
    public List<Map<String, String>> buildContext(Long sessionId, String systemPrompt, int maxContext) {
        // 1) 取最近 N 条（先多取，按 token 裁剪）
        int fetchLimit = Math.min(50, maxContext / 2);
        List<Map<String, String>> recent = memory.recent(sessionId, fetchLimit);

        // 2) system 占 token 预算
        List<Map<String, String>> out = new ArrayList<>();
        int usedTokens = 0;
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            out.add(sys);
            usedTokens += approxTokens(systemPrompt);
        }

        // 3) 留 20% 给输出
        int inputBudget = (int) (maxContext * 0.8) - usedTokens;
        if (inputBudget < 100) inputBudget = 100;

        // 4) 倒序填充（最近的优先），但最后输出时再倒过来
        List<Map<String, String>> reversed = new ArrayList<>(recent);
        java.util.Collections.reverse(reversed);

        List<Map<String, String>> picked = new ArrayList<>();
        for (Map<String, String> m : reversed) {
            int t = approxTokens(m.get("content"));
            if (usedTokens + t > inputBudget) {
                log.debug("上下文超 budget，跳过 message (used={}, t={}, budget={})", usedTokens, t, inputBudget);
                continue;
            }
            picked.add(m);
            usedTokens += t;
        }

        // 5) 反转回时间正序
        java.util.Collections.reverse(picked);
        out.addAll(picked);

        log.info("构建 context: sessionId={} maxContext={} inputBudget={} used={} picked={} systemLen={}",
                sessionId, maxContext, inputBudget, usedTokens, picked.size(),
                systemPrompt == null ? 0 : systemPrompt.length());

        return out;
    }

    /**
     * 粗估 token 数。中文/英文混合的经验值。
     * 真实生产应替换为 jtokkit / tiktoken。
     */
    public static int approxTokens(String s) {
        if (s == null || s.isEmpty()) return 0;
        // 1 char ≈ 0.6 token（混合中英），最小 1
        int cjk = 0, ascii = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 128) ascii++;
            else cjk++;
        }
        return Math.max(1, cjk + (int) Math.ceil(ascii / 4.0));
    }
}
