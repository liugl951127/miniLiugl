package com.bank.dualrecord.config;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器 - 基于 Guava RateLimiter
 *
 * <p>按接口+用户限流,防刷
 */
@Slf4j
@Component
public class RateLimitConfig {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    /**
     * 默认限流:每秒 10 个令牌
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 10);
    }

    /**
     * 自定义限流(每秒令牌数)
     */
    public boolean tryAcquire(String key, double permitsPerSecond) {
        return limiters.computeIfAbsent(key, k -> RateLimiter.create(permitsPerSecond))
            .tryAcquire();
    }

    /**
     * 重置某个 key 的限流器
     */
    public void reset(String key) {
        limiters.remove(key);
    }
}
