package com.minimax.memory.summary;

import com.minimax.memory.context.ContextBuilder;
import com.minimax.memory.shortterm.ShortTermMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 长会话摘要压缩器。
 *
 * 触发条件：会话消息数 > SUMMARY_THRESHOLD
 * 策略（首版 - Day 6 简化版）：
 *  - 取最早 10 条消息
 *  - 截取每条 content 的前 60 字符拼接成"摘要"（占位实现）
 *  - Day 7 升级为真实 LLM 摘要（用 OpenAiCompatibleAdapter 调 model）
 *
 * Redis 存储: summary:sess:{id} = "{summary}|{summaryAt}|{fromMsgId}"
 */
@Slf4j
@Component
public class Summarizer {

    public static final int SUMMARY_THRESHOLD = 30;   // > 30 条触发
    public static final int KEEP_RECENT = 10;          // 保留最近 10 条
    public static final int SUMMARY_BATCH = 10;        // 摘要前 10 条
    public static final int CONTENT_TRUNCATE = 60;     // 摘要时每条截 60 字符

    private static final String KEY_PREFIX = "summary:sess:";
    private static final Duration TTL = Duration.ofDays(30);

    private final ShortTermMemory memory;
    private final StringRedisTemplate redis;  // 可能为 null（test profile 禁了 Redis）

    @Autowired
    public Summarizer(ShortTermMemory memory,
                       @Autowired(required = false) StringRedisTemplate redis) {
        this.memory = memory;
        this.redis = redis;
        if (redis == null) log.warn("Summarizer: Redis 不可用，摘要只写内存（重启丢失）");
    }

    /**
     * 检查并按需压缩。
     * @return true 表示已压缩，false 表示不需要。
     */
    public boolean maybeSummarize(Long sessionId) {
        long total = memory.size(sessionId);
        if (total <= SUMMARY_THRESHOLD) return false;

        log.info("触发摘要: sessionId={} totalMessages={}", sessionId, total);
        try {
            // 1) 拉最早的 SUMMARY_BATCH 条
            List<Map<String, String>> oldest = memory.recent(sessionId, (int) total);
            if (oldest.size() <= KEEP_RECENT) return false;
            List<Map<String, String>> toSummarize = oldest.subList(0, Math.min(SUMMARY_BATCH, oldest.size() - KEEP_RECENT));

            // 2) 拼接成摘要文本（占位实现：截前 60 字符）
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> m : toSummarize) {
                String content = m.getOrDefault("content", "");
                String role = m.getOrDefault("role", "?");
                String trunc = content.length() > CONTENT_TRUNCATE
                        ? content.substring(0, CONTENT_TRUNCATE) + "..." : content;
                sb.append("[").append(role).append("] ").append(trunc).append("\n");
            }
            String summary = sb.toString();
            long tokenEst = ContextBuilder.approxTokens(summary);
            log.info("摘要生成: sessionId={} 原 {} 条 → {} 字符 / ~{} tokens",
                    sessionId, toSummarize.size(), summary.length(), tokenEst);

            // 3) 存到 Redis
            String key = KEY_PREFIX + sessionId;
            if (redis != null) {
                redis.opsForValue().set(key, summary, TTL);
            }

            // 4) 从短期记忆中清除已被摘要的消息
            //   简化实现：保留最近 KEEP_RECENT 条 + 摘要作为 system 注入
            //   Day 7 会做精确的 msg-id 级别摘要
            memory.clear(sessionId);
            // 重新写入最近 KEEP_RECENT 条（重建）
            // 注意：这里丢失了"已被摘要"的消息，Day 7 升级用 list 截断而非 clear
            int from = Math.max(0, oldest.size() - KEEP_RECENT);
            for (int i = from; i < oldest.size(); i++) {
                Map<String, String> m = oldest.get(i);
                memory.append(sessionId, m.get("role"), m.get("content"));
            }

            return true;
        } catch (Exception e) {
            log.error("摘要失败: sessionId={}", sessionId, e);
            return false;
        }
    }

    /** 读取已存在的摘要（如有）。 */
    public String getSummary(Long sessionId) {
        if (redis == null) return null;
        try {
            return redis.opsForValue().get(KEY_PREFIX + sessionId);
        } catch (Exception e) {
            return null;
        }
    }
}
