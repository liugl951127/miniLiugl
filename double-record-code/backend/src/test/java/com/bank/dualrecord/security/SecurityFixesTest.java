package com.bank.dualrecord.security;

import com.bank.dualrecord.fabric.event.NonceIdempotentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全修复单元测试
 *
 * <p>覆盖:
 * <ul>
 *   <li>DRL-2026-001:分布式限流器(Lua 脚本调用)
 *   <li>DRL-2026-002:JWT 撤销 + 强制下线
 *   <li>DRL-2026-003:Actuator IP 白名单(CIDR 匹配)
 *   <li>DRL-2026-005:链码事件 nonce 幂等
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SecurityFixesTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    // ============================================================
    // DRL-2026-005:Nonce 幂等
    // ============================================================

    @Test
    void testNonceIdempotent_firstCallAllowed() {
        NonceIdempotentService service = new NonceIdempotentService(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("chain:event:nonce:N1"), eq("1"), any())).thenReturn(true);

        assertTrue(service.tryAcquire("N1"));
    }

    @Test
    void testNonceIdempotent_duplicateRejected() {
        NonceIdempotentService service = new NonceIdempotentService(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("chain:event:nonce:N2"), eq("1"), any())).thenReturn(false);

        assertFalse(service.tryAcquire("N2"));
    }

    @Test
    void testNonceIdempotent_nullNoncePass() {
        NonceIdempotentService service = new NonceIdempotentService(redis);
        // null nonce 不阻塞(兼容老链码)
        assertTrue(service.tryAcquire(null));
        assertTrue(service.tryAcquire(""));
    }

    @Test
    void testNonceIdempotent_redisFailurePass() {
        NonceIdempotentService service = new NonceIdempotentService(redis);
        when(redis.opsForValue()).thenThrow(new RuntimeException("Redis down"));
        // 降级放行
        assertTrue(service.tryAcquire("N3"));
    }

    // ============================================================
    // DRL-2026-001:分布式限流
    // ============================================================

    @Test
    void testDistributedRateLimiter_fixedWindow_allowed() {
        DistributedRateLimiter limiter = new DistributedRateLimiter();
        ReflectionTestUtils.setField(limiter, "redis", redis);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),
            anyList(), any(Object[].class)))
            .thenReturn(java.util.Arrays.asList(1L, 1L, 5L, 60L));

        DistributedRateLimiter.RateLimitResult result = limiter.tryAcquireFixed("rl:test:1", 5, 60);
        assertTrue(result.allowed);
        assertEquals(1, result.current);
        assertEquals(5, result.limit);
    }

    @Test
    void testDistributedRateLimiter_fixedWindow_blocked() {
        DistributedRateLimiter limiter = new DistributedRateLimiter();
        ReflectionTestUtils.setField(limiter, "redis", redis);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),
            anyList(), any(Object[].class)))
            .thenReturn(java.util.Arrays.asList(0L, 6L, 5L, 30L));

        DistributedRateLimiter.RateLimitResult result = limiter.tryAcquireFixed("rl:test:2", 5, 60);
        assertFalse(result.allowed);
        assertEquals(6, result.current);
        assertEquals(30, result.retryAfter);
    }

    @Test
    void testDistributedRateLimiter_redisFailurePass() {
        DistributedRateLimiter limiter = new DistributedRateLimiter();
        ReflectionTestUtils.setField(limiter, "redis", redis);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),
            anyList(), any(Object[].class)))
            .thenThrow(new RuntimeException("Redis down"));

        DistributedRateLimiter.RateLimitResult result = limiter.tryAcquireFixed("rl:test:3", 5, 60);
        // 降级放行
        assertTrue(result.allowed);
    }

    @Test
    void testDistributedRateLimiter_tokenBucket() {
        DistributedRateLimiter limiter = new DistributedRateLimiter();
        ReflectionTestUtils.setField(limiter, "redis", redis);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),
            anyList(), any(Object[].class)))
            .thenReturn(java.util.Arrays.asList(1L, 9L, 10L));

        DistributedRateLimiter.RateLimitResult result = limiter.tryAcquireTokenBucket("rl:tb:1", 10, 1.0);
        assertTrue(result.allowed);
    }

    // ============================================================
    // DRL-2026-002:JWT 撤销
    // ============================================================

    @Test
    void testJwtTokenManager_issueToken() {
        JwtTokenManager mgr = new JwtTokenManager(redis);
        ReflectionTestUtils.setField(mgr, "jwtSecret", "test_secret_at_least_32_chars_long_for_hmac");
        ReflectionTestUtils.setField(mgr, "expiration", 7200L);
        ReflectionTestUtils.setField(mgr, "issuer", "test-issuer");

        String token = mgr.issue("user001", "ADMIN", "BJ001");
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void testJwtTokenManager_revoke() {
        JwtTokenManager mgr = new JwtTokenManager(redis);
        ReflectionTestUtils.setField(mgr, "jwtSecret", "test_secret_at_least_32_chars_long_for_hmac");
        ReflectionTestUtils.setField(mgr, "expiration", 7200L);
        ReflectionTestUtils.setField(mgr, "issuer", "test-issuer");

        String token = mgr.issue("user002", "USER", "BJ001");
        when(redis.opsForValue()).thenReturn(valueOps);

        // revoke 会计算 TTL
        assertDoesNotThrow(() -> mgr.revoke(token));
        verify(valueOps, atLeastOnce()).set(anyString(), eq("1"), any());
    }

    @Test
    void testJwtTokenManager_isRevoked() {
        JwtTokenManager mgr = new JwtTokenManager(redis);
        when(redis.hasKey("jwt:revoked:test-jti-1")).thenReturn(true);

        assertTrue(mgr.isRevoked("test-jti-1"));
    }

    @Test
    void testJwtTokenManager_revokeAllForUser() {
        JwtTokenManager mgr = new JwtTokenManager(redis);
        when(redis.opsForValue()).thenReturn(valueOps);

        mgr.revokeAllForUser("user003");
        verify(valueOps).set(eq("jwt:revoked:user:user003"), eq("1"), any());
    }

    // ============================================================
    // DRL-2026-003:Actuator IP 白名单
    // ============================================================

    @Test
    void testCidrMatch_8_bit() throws Exception {
        // 测试 10.0.0.0/8 应匹配 10.x.x.x
        java.lang.reflect.Method m = ActuatorSecurityConfig.ActuatorIpFilter.class
            .getDeclaredMethod("matchCidr", String.class, String.class);
        m.setAccessible(true);
        ActuatorSecurityConfig.ActuatorIpFilter filter =
            new ActuatorSecurityConfig.ActuatorIpFilter(java.util.Arrays.asList("10.0.0.0/8"));

        assertTrue((Boolean) m.invoke(filter, "10.1.2.3", "10.0.0.0/8"));
        assertTrue((Boolean) m.invoke(filter, "10.255.255.255", "10.0.0.0/8"));
        assertFalse((Boolean) m.invoke(filter, "11.0.0.0", "10.0.0.0/8"));
    }

    @Test
    void testCidrMatch_24_bit() throws Exception {
        java.lang.reflect.Method m = ActuatorSecurityConfig.ActuatorIpFilter.class
            .getDeclaredMethod("matchCidr", String.class, String.class);
        m.setAccessible(true);

        assertTrue((Boolean) m.invoke(null, "192.168.1.100", "192.168.1.0/24"));
        assertFalse((Boolean) m.invoke(null, "192.168.2.100", "192.168.1.0/24"));
    }

    @Test
    void testCidrMatch_16_bit() throws Exception {
        java.lang.reflect.Method m = ActuatorSecurityConfig.ActuatorIpFilter.class
            .getDeclaredMethod("matchCidr", String.class, String.class);
        m.setAccessible(true);

        assertTrue((Boolean) m.invoke(null, "172.16.0.1", "172.16.0.0/16"));
        assertTrue((Boolean) m.invoke(null, "172.16.255.254", "172.16.0.0/16"));
        assertFalse((Boolean) m.invoke(null, "172.17.0.1", "172.16.0.0/16"));
    }
}
