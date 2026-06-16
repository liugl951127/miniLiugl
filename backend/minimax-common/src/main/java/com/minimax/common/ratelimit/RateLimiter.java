package com.minimax.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 多维度限流器 (Bucket4j 封装)。
 *
 * 支持 3 种 scope:
 *  - GLOBAL: 全局共享一个桶
 *  - IP:    每个 IP 一个桶
 *  - USER:  每个 user 一个桶
 *
 * 用法:
 *   if (!rateLimiter.tryConsume("ip:" + clientIp, 1)) {
 *       throw new BizException(ResultCode.RATE_LIMIT);
 *   }
 */
public class RateLimiter {

    /** 默认配置: 100 容量 / 60s 补充 100 */
    public static final int DEFAULT_CAPACITY = 100;
    public static final int DEFAULT_REFILL = 100;
    public static final int DEFAULT_REFILL_PERIOD_SECONDS = 60;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillTokens;
    private final Duration refillPeriod;

    public RateLimiter() {
        this(DEFAULT_CAPACITY, DEFAULT_REFILL, DEFAULT_REFILL_PERIOD_SECONDS);
    }

    public RateLimiter(int capacity, int refillTokens, int refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
    }

    public Bucket bucketFor(String key) {
        return buckets.computeIfAbsent(key, k -> newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(refillTokens, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }

    public boolean tryConsume(String key, long tokens) {
        return bucketFor(key).tryConsume(tokens);
    }

    public boolean tryConsume(String key) {
        return tryConsume(key, 1);
    }

    public long availableTokens(String key) {
        return bucketFor(key).getAvailableTokens();
    }

    public void clear() {
        buckets.clear();
    }

    public int size() {
        return buckets.size();
    }
}
