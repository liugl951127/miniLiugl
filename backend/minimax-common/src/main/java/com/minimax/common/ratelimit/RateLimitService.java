package com.minimax.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 多 scope 限流服务。
 *  - ip 限流
 *  - user 限流
 *  - global 限流
 *
 * 不同 scope 用不同 RateLimiter 实例。
 */
@Slf4j
@Component
public class RateLimitService {

    @Value("${minimax.ratelimit.ip.capacity:100}")
    private int ipCapacity = 100;
    @Value("${minimax.ratelimit.ip.refill:100}")
    private int ipRefill = 100;
    @Value("${minimax.ratelimit.ip.period-seconds:60}")
    private int ipPeriod = 60;

    @Value("${minimax.ratelimit.user.capacity:60}")
    private int userCapacity = 60;
    @Value("${minimax.ratelimit.user.refill:60}")
    private int userRefill = 60;
    @Value("${minimax.ratelimit.user.period-seconds:60}")
    private int userPeriod = 60;

    @Value("${minimax.ratelimit.global.capacity:1000}")
    private int globalCapacity = 1000;
    @Value("${minimax.ratelimit.global.refill:1000}")
    private int globalRefill = 1000;
    @Value("${minimax.ratelimit.global.period-seconds:60}")
    private int globalPeriod = 60;

    private final ConcurrentMap<String, RateLimiter> scopedLimiters = new ConcurrentHashMap<>();

    public boolean tryAcquire(String scope, String key) {
        if (key == null) return true;  // 未识别用户放行
        RateLimiter limiter = scopedLimiters.computeIfAbsent(scope, s -> {
            switch (s) {
                case "ip":     return new RateLimiter(ipCapacity, ipRefill, ipPeriod);
                case "user":   return new RateLimiter(userCapacity, userRefill, userPeriod);
                case "global": return new RateLimiter(globalCapacity, globalRefill, globalPeriod);
                default:       return new RateLimiter();
            }
        });
        boolean ok = limiter.tryConsume(key);
        if (!ok) {
            log.warn("RateLimit 触发: scope={} key={} remaining={}",
                    scope, key, limiter.availableTokens(key));
        }
        return ok;
    }

    public boolean tryAcquireIp(String ip) {
        return tryAcquire("ip", ip);
    }

    public boolean tryAcquireUser(Long userId) {
        return tryAcquire("user", userId == null ? null : "u:" + userId);
    }

    public boolean tryAcquireGlobal() {
        return tryAcquire("global", "global");
    }
}
