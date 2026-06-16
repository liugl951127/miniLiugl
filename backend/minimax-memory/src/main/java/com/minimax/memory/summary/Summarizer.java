package com.minimax.memory.summary;

import com.minimax.memory.shortterm.ShortTermMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 长会话摘要压缩器（Day 7 升级：用 LlmSummarizer 真实摘要）。
 *
 * 触发条件：会话消息数 > SUMMARY_THRESHOLD
 * 策略：
 *  1) 拉最近的消息
 *  2) LlmSummarizer 调用 model 服务生成摘要
 *  3) 摘要存 Redis (test 模式没 Redis → 内存丢失)
 *  4) 清空短期记忆，保留最近 KEEP_RECENT 条
 */
@Slf4j
@Component
public class Summarizer {

    public static final int SUMMARY_THRESHOLD = 30;
    public static final int KEEP_RECENT = 10;
    public static final int SUMMARY_BATCH = 20;

    private static final String KEY_PREFIX = "summary:sess:";
    private static final Duration TTL = Duration.ofDays(30);

    private final ShortTermMemory memory;
    private final LlmSummarizer llmSummarizer;
    private final StringRedisTemplate redis;

    @Autowired
    public Summarizer(ShortTermMemory memory,
                       LlmSummarizer llmSummarizer,
                       @Autowired(required = false) StringRedisTemplate redis) {
        this.memory = memory;
        this.llmSummarizer = llmSummarizer;
        this.redis = redis;
        if (redis == null) log.warn("Summarizer: Redis 不可用，摘要只写内存");
    }

    public boolean maybeSummarize(Long sessionId) {
        long total = memory.size(sessionId);
        if (total <= SUMMARY_THRESHOLD) return false;

        log.info("触发摘要: sessionId={} totalMessages={}", sessionId, total);
        try {
            String summary = llmSummarizer.summarize(sessionId, SUMMARY_BATCH);
            if (summary == null || summary.isBlank()) {
                log.warn("摘要为空，跳过");
                return false;
            }
            String key = KEY_PREFIX + sessionId;
            if (redis != null) {
                redis.opsForValue().set(key, summary, TTL);
            }

            // 重建短期记忆：保留最近 KEEP_RECENT
            List<Map<String, String>> old = memory.recent(sessionId, (int) total);
            memory.clear(sessionId);
            int from = Math.max(0, old.size() - KEEP_RECENT);
            for (int i = from; i < old.size(); i++) {
                Map<String, String> m = old.get(i);
                memory.append(sessionId, m.get("role"), m.get("content"));
            }
            return true;
        } catch (Exception e) {
            log.error("摘要失败: sessionId={}", sessionId, e);
            return false;
        }
    }

    public String getSummary(Long sessionId) {
        if (redis == null) return null;
        try { return redis.opsForValue().get(KEY_PREFIX + sessionId); }
        catch (Exception e) { return null; }
    }
}
