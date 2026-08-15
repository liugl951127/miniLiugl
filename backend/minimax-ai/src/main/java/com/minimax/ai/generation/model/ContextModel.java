package com.minimax.ai.generation.model;

import com.minimax.ai.generation.ConversationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 上下文意图模型 (V3.5.15+)
 *
 * <h3>原理</h3>
 * 利用会话历史 (ConversationContext) 推断当前 query 的 intent.
 * 逻辑: 如果上一轮是 GENERATE_CHART, 这次 "再画一次" 大概率还是 GENERATE_CHART
 *
 * <h3>3 种上下文信号</h3>
 * <ul>
 *   <li><b>代词指代</b>: query 含 "它/他/她/这个/那个" → 上一轮 intent +0.5</li>
 *   <li><b>承接词</b>: "再来一个/重新/也" → 上一轮 intent +0.3</li>
 *   <li><b>短 query</b>: query 长度 &lt; 5 字 + 无关键词 → 上一轮 intent +0.2</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextModel {

    private final ConversationContext conversationContext;

    /** 代词 / 指示词 → 触发上一轮 intent */
    private static final String[] PRONOUNS = {
        "它", "他", "她", "这个", "那个", "这", "那", "再", "its", "it", "this", "that", "same"
    };

    /** 承接词 (继续做某事) */
    private static final String[] CONTINUATION_WORDS = {
        "再", "也", "再来", "重新", "再次", "再一次", "again", "too", "also", "another"
    };

    /**
     * 推理: 基于上一轮上下文给当前 query 加分
     *
     * @param query 当前 query
     * @param sessionId 会话 ID
     * @return intent → 加分 (0~0.5, 不超过 0.5 避免压过 TF)
     */
    public Map<String, Double> score(String query, String sessionId) {
        if (query == null || sessionId == null) {
            return Collections.emptyMap();
        }

        // 取上一轮 intent (用现有 recentIntents API)
        List<com.minimax.ai.generation.KeywordEngine.Intent> last =
                conversationContext.recentIntents(sessionId, 1);
        if (last.isEmpty()) {
            return Collections.emptyMap();
        }
        String lastIntent = last.get(0).name();
        if ("CHAT".equals(lastIntent) || "UNKNOWN".equals(lastIntent)) {
            return Collections.emptyMap();
        }

        String lower = query.toLowerCase();
        double boost = 0.0;

        // 1. 代词指代
        for (String p : PRONOUNS) {
            if (lower.contains(p)) {
                boost = Math.max(boost, 0.5);
                break;
            }
        }

        // 2. 承接词
        if (boost == 0.0) {
            for (String c : CONTINUATION_WORDS) {
                if (lower.contains(c)) {
                    boost = Math.max(boost, 0.3);
                    break;
                }
            }
        }

        // 3. 短 query 无关键词 (粗略, 不查 keyword)
        if (boost == 0.0 && query.length() < 5) {
            boost = 0.2;
        }

        if (boost > 0.0) {
            log.debug("[context] session={} query='{}' lastIntent={} boost={}",
                    sessionId, query, lastIntent, boost);
            return Collections.singletonMap(lastIntent, boost);
        }
        return Collections.emptyMap();
    }
}
