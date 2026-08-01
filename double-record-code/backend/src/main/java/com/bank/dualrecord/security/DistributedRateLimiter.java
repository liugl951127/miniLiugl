package com.bank.dualrecord.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式限流器 - 基于 Redis + Lua 脚本(原子操作)
 *
 * <p>修复 DRL-2026-001:分布式限流绕过
 * <p>替代旧的 Guava RateLimiter(单 JVM 内存)
 *
 * <p>Lua 脚本保证 INCR + EXPIRE 原子性,避免竞态
 * <p>支持:固定窗口 / 滑动窗口 / 令牌桶 3 种算法
 *
 * @author Mavis
 */
@Slf4j
@Component
public class DistributedRateLimiter {

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 固定窗口算法(最简单)
     *
     * @param key    限流 key(如 "rl:login:192.168.1.1")
     * @param limit  时间窗内允许的请求数
     * @param period 时间窗(秒)
     * @return true=通过,false=被限流
     */
    private static final String FIXED_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local period = tonumber(ARGV[2])

        local cur = redis.call('INCR', key)
        if cur == 1 then
            redis.call('EXPIRE', key, period)
        end

        if cur > limit then
            local ttl = redis.call('TTL', key)
            return {0, cur, limit, ttl}
        end
        return {1, cur, limit, period}
    """;

    /**
     * 滑动窗口算法(更精准,推荐用于关键接口)
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])

        -- 移除窗口外的请求
        redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window * 1000)
        -- 当前窗口请求数
        local cur = redis.call('ZCARD', key)
        if cur >= limit then
            return {0, cur, limit, 0}
        end
        -- 添加当前请求
        redis.call('ZADD', key, now, now .. ':' .. math.random())
        redis.call('EXPIRE', key, window)
        return {1, cur + 1, limit, 0}
    """;

    /**
     * 令牌桶算法(支持突发流量)
     */
    private static final String TOKEN_BUCKET_SCRIPT = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])

        local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
        local tokens = tonumber(bucket[1]) or capacity
        local last_refill = tonumber(bucket[2]) or now

        -- 补充令牌
        local elapsed = math.max(0, now - last_refill)
        local refill = elapsed * rate / 1000
        tokens = math.min(capacity, tokens + refill)

        local allowed = 0
        if tokens >= requested then
            tokens = tokens - requested
            allowed = 1
        end

        redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
        redis.call('EXPIRE', key, math.ceil(capacity / rate) + 1)
        return {allowed, math.floor(tokens), capacity}
    """;

    private final DefaultRedisScript<List> fixedScript = new DefaultRedisScript<>(FIXED_WINDOW_SCRIPT, List.class);
    private final DefaultRedisScript<List> slidingScript = new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT, List.class);
    private final DefaultRedisScript<List> tokenScript = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, List.class);

    /**
     * 固定窗口限流
     */
    public RateLimitResult tryAcquireFixed(String key, int limit, int periodSeconds) {
        try {
            List<Long> result = redis.execute(fixedScript,
                Arrays.asList(key),
                String.valueOf(limit), String.valueOf(periodSeconds));
            if (result == null || result.isEmpty()) {
                return RateLimitResult.allowed(0, limit);
            }
            boolean allowed = result.get(0) == 1L;
            long current = result.get(1);
            long ttl = result.size() > 3 ? result.get(3) : periodSeconds;
            return new RateLimitResult(allowed, current, limit, ttl);
        } catch (Exception e) {
            log.error("限流异常(放行): {}", e.getMessage());
            return RateLimitResult.allowed(0, limit); // 失败放行,降级
        }
    }

    /**
     * 滑动窗口限流(推荐)
     */
    public RateLimitResult tryAcquireSliding(String key, int limit, int windowSeconds) {
        try {
            long now = System.currentTimeMillis();
            List<Long> result = redis.execute(slidingScript,
                Arrays.asList(key),
                String.valueOf(now), String.valueOf(windowSeconds), String.valueOf(limit));
            if (result == null || result.isEmpty()) {
                return RateLimitResult.allowed(0, limit);
            }
            boolean allowed = result.get(0) == 1L;
            long current = result.get(1);
            return new RateLimitResult(allowed, current, limit, windowSeconds);
        } catch (Exception e) {
            log.error("滑动窗口限流异常: {}", e.getMessage());
            return RateLimitResult.allowed(0, limit);
        }
    }

    /**
     * 令牌桶限流(允许突发)
     */
    public RateLimitResult tryAcquireTokenBucket(String key, int capacity, double refillRatePerSecond) {
        try {
            long now = System.currentTimeMillis();
            List<Long> result = redis.execute(tokenScript,
                Arrays.asList(key),
                String.valueOf(capacity), String.valueOf(refillRatePerSecond),
                String.valueOf(now), "1");
            if (result == null || result.isEmpty()) {
                return RateLimitResult.allowed(0, capacity);
            }
            boolean allowed = result.get(0) == 1L;
            long remaining = result.get(1);
            return new RateLimitResult(allowed, capacity - remaining, capacity, 0);
        } catch (Exception e) {
            log.error("令牌桶限流异常: {}", e.getMessage());
            return RateLimitResult.allowed(0, capacity);
        }
    }

    /**
     * 重置限流
     */
    public void reset(String key) {
        redis.delete(key);
    }

    /**
     * 限流结果
     */
    public static class RateLimitResult {
        public final boolean allowed;
        public final long current;
        public final long limit;
        public final long retryAfter;

        public RateLimitResult(boolean allowed, long current, long limit, long retryAfter) {
            this.allowed = allowed;
            this.current = current;
            this.limit = limit;
            this.retryAfter = retryAfter;
        }

        public static RateLimitResult allowed(long current, long limit) {
            return new RateLimitResult(true, current, limit, 0);
        }

        public long getRemaining() {
            return Math.max(0, limit - current);
        }
    }
}
