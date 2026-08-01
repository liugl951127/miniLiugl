package com.bank.dualrecord.fabric.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 链码事件 nonce 幂等服务 - 修复 DRL-2026-005
 *
 * <p>消费者用 Redis SETNX 防止同一事件被处理多次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonceIdempotentService {

    private final StringRedisTemplate redis;
    private static final String NONCE_PREFIX = "chain:event:nonce:";
    private static final Duration NONCE_TTL = Duration.ofHours(24);

    /**
     * 检查并占用 nonce
     *
     * @return true=新事件(可处理), false=已处理过(应跳过)
     */
    public boolean tryAcquire(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return true; // 没有 nonce 时不阻塞(兼容老版本链码)
        }
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(
                NONCE_PREFIX + nonce,
                "1",
                NONCE_TTL
            );
            if (Boolean.FALSE.equals(ok)) {
                log.warn("事件重放检测: nonce={} 已存在,跳过", nonce);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("nonce 校验异常(放行): {}", e.getMessage());
            return true; // 降级放行,避免 Redis 故障阻塞业务
        }
    }
}
