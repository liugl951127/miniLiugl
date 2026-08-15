package com.minimax.chat.memory_ext.shortterm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 短期记忆 - 存储会话最近 N 轮消息。
 *
 * 存储策略:
 *  1. 优先 Redis LIST:  key=stm:sess:{sessionId}  value=JSON 数组
 *  2. Redis 不可用时降级到 Caffeine 本地缓存
 *  3. 容量上限 100 条，超出 LTRIM 裁剪
 *  4. 默认 TTL 7 天
 *
 * 用途：给 chat 调模型前，按模型 maxContext 裁剪最近的 N 条消息
 */
@Slf4j
@Component
public class ShortTermMemory {

    private static final String KEY_PREFIX = "stm:sess:";
    private static final int MAX_MESSAGES = 100;
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();
    private final boolean redisAvailable;

    /** 本地缓存：sessionId -> List<Map<role, content>>。Redis 挂了时兜底。 */
    private final Cache<String, List<String>> fallback = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Autowired
    public ShortTermMemory(@Autowired(required = false) StringRedisTemplate redis,
                           @Value("${minimax.memory.use-redis:true}") boolean useRedis) {
        this.redis = redis;
        this.redisAvailable = useRedis && redis != null;
        if (!this.redisAvailable) {
            log.warn("ShortTermMemory: Redis 不可用，启用 Caffeine 本地缓存");
        }
    }

    /**
     * 追加一条消息到会话。
     * @param sessionId 会话 ID
     * @param role      user / assistant / system
     * @param content   内容
     */
    public void append(Long sessionId, String role, String content) {
        if (sessionId == null || role == null || content == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "参数不能为空");
        }
        Map<String, String> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        msg.put("ts", String.valueOf(System.currentTimeMillis()));
        String jsonStr;
        try {
            jsonStr = json.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.FAIL, "序列化失败: " + e.getMessage());
        }
        String key = key(sessionId);
        if (redisAvailable) {
            try {
                redis.opsForList().rightPush(key, jsonStr);
                redis.opsForList().trim(key, -MAX_MESSAGES, -1);
                redis.expire(key, DEFAULT_TTL);
                return;
            } catch (Exception e) {
                log.warn("Redis 写入失败，降级到本地缓存: {}", e.getMessage());
            }
        }
        // 兜底
        List<String> list = fallback.get(key, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(jsonStr);
            while (list.size() > MAX_MESSAGES) list.remove(0);
        }
    }

    /**
     * 拉取会话最近 N 条消息（按时间升序）。
     */
    public List<Map<String, String>> recent(Long sessionId, int limit) {
        if (limit <= 0 || limit > MAX_MESSAGES) limit = 20;
        String key = key(sessionId);
        List<String> raw;
        if (redisAvailable) {
            try {
                Long sizeBox = redis.opsForList().size(key);
                if (sizeBox == null || sizeBox == 0) return List.of();
                long size = sizeBox;
                long start = Math.max(0, size - limit);
                raw = redis.opsForList().range(key, start, -1);
                if (raw == null) return List.of();
            } catch (Exception e) {
                log.warn("Redis 读取失败，降级本地: {}", e.getMessage());
                raw = fallbackGet(key, limit);
            }
        } else {
            raw = fallbackGet(key, limit);
        }
        return deserialize(raw);
    }

    /** 清空会话短期记忆。 */
    public void clear(Long sessionId) {
        String key = key(sessionId);
        if (redisAvailable) {
            try { redis.delete(key); } catch (Exception ignore) {}
        }
        fallback.invalidate(key);
    }

    /** 统计消息数。 */
    public long size(Long sessionId) {
        String key = key(sessionId);
        if (redisAvailable) {
            try {
                Long s = redis.opsForList().size(key);
                return s == null ? 0 : s;
            } catch (Exception ignore) {}
        }
        List<String> list = fallback.getIfPresent(key);
        return list == null ? 0 : list.size();
    }

    // ---------- helpers ----------

    private List<String> fallbackGet(String key, int limit) {
        List<String> list = fallback.getIfPresent(key);
        if (list == null) return List.of();
        synchronized (list) {
            int from = Math.max(0, list.size() - limit);
            return new ArrayList<>(list.subList(from, list.size()));
        }
    }

    private List<Map<String, String>> deserialize(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<Map<String, String>> out = new ArrayList<>(raw.size());
        for (String s : raw) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> m = json.readValue(s, Map.class);
                out.add(m);
            } catch (Exception e) {
                log.warn("反序列化记忆失败: {}", e.getMessage());
            }
        }
        return out;
    }

    private String key(Long sessionId) { return KEY_PREFIX + sessionId; }
}
