package com.minimax.chat.memory_ext.context;

import com.minimax.chat.memory_ext.longterm.LongTermMemoryService;
import com.minimax.chat.memory_ext.pref.UserPref;
import com.minimax.chat.memory_ext.pref.UserPrefService;
import com.minimax.chat.memory_ext.shortterm.ShortTermMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨会话上下文构建（Day 7 核心）：
 * 把短期记忆 + 长期记忆召回 + 用户偏好 + 摘要 拼成最终 messages。
 *
 * 输出顺序（重要性降序）：
 *  1) system prompt
 *  2) 摘要（如果有）
 *  3) 用户偏好（"我之前说过我喜欢 X"）
 *  4) 跨会话召回的高相关记忆（"我们上次聊过 X"）
 *  5) 本会话最近 N 条
 *  6) 当前问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossSessionContextBuilder {

    private final ShortTermMemory shortTerm;
    private final LongTermMemoryService longTerm;
    private final UserPrefService prefs;
    private final ContextBuilder inner;

    /**
     * @param userId        用户
     * @param sessionId     当前会话
     * @param systemPrompt  系统提示词
     * @param maxContext    模型 max_context tokens
     * @param recallTopK    跨会话召回数量
     * @return 拼好的 messages
     */
    public List<Map<String, String>> build(Long userId, Long sessionId,
                                            String systemPrompt, int maxContext, int recallTopK) {
        List<Map<String, String>> out = new ArrayList<>();
        int budget = (int) (maxContext * 0.8);
        int used = 0;

        // 1) system prompt
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            out.add(msg("system", systemPrompt));
            used += ContextBuilder.approxTokens(systemPrompt);
        }

        // 2) 用户偏好（拼成"我之前的偏好：" system 风格）
        List<UserPref> userPrefs = prefs.listByUser(userId);
        if (!userPrefs.isEmpty()) {
            StringBuilder sb = new StringBuilder("用户偏好：");
            for (UserPref p : userPrefs) {
                sb.append(p.getPrefKey()).append("=").append(p.getPrefValue()).append("; ");
            }
            String prefText = sb.toString();
            if (ContextBuilder.approxTokens(prefText) + used < budget) {
                out.add(msg("system", prefText));
                used += ContextBuilder.approxTokens(prefText);
            }
        }

        // 3) 跨会话记忆召回（基于"最近一条 user 消息"做 query）
        List<Map<String, String>> recent = shortTerm.recent(sessionId, 5);
        String query = recent.isEmpty() ? null : recent.get(recent.size() - 1).get("content");
        if (query != null && !query.isBlank()) {
            List<LongTermMemoryService.RecallHit> hits = longTerm.recall(userId, query, recallTopK);
            if (!hits.isEmpty()) {
                StringBuilder sb = new StringBuilder("用户相关的历史记忆（可能相关）：");
                int i = 0;
                for (var h : hits) {
                    if (ContextBuilder.approxTokens(sb.toString()) + used > budget) break;
                    String snippet = h.summary() != null ? h.summary() : truncate(h.content(), 80);
                    sb.append("\n- ").append(snippet).append(" (相似度 ").append(String.format("%.2f", h.score())).append(")");
                    i++;
                }
                if (i > 0) {
                    out.add(msg("system", sb.toString()));
                    used += ContextBuilder.approxTokens(sb.toString());
                }
            }
        }

        // 4) 本会话短期记忆
        List<Map<String, String>> shortMsgs = shortTerm.recent(sessionId, 20);
        for (Map<String, String> m : shortMsgs) {
            int t = ContextBuilder.approxTokens(m.get("content"));
            if (used + t > budget) break;
            out.add(m);
            used += t;
        }

        log.info("跨会话 context: userId={} sessionId={} out={} usedTokens={}",
                userId, sessionId, out.size(), used);
        return out;
    }

    private Map<String, String> msg(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private String truncate(String s, int n) {
        return s == null ? "" : (s.length() > n ? s.substring(0, n) + "..." : s);
    }
}
