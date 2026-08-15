package com.minimax.model.quota;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Bucket4j 的内存限流器。
 * - 每个用户一个令牌桶：默认 60 req / minute
 * - 超过返回 false，调用方应 429
 *
 * 生产建议改 Redis 分布式版（Caffeine 集群下会丢计数）。
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long userId) {
        Bucket b = buckets.computeIfAbsent(userId, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(10)                              // 突发容量 10
                                .refillGreedy(60, Duration.ofMinutes(1))   // 长期 60/min
                                .build())
                        .build()
        );
        return b.tryConsume(1);
    }
}
