package com.minimax.common;

import com.minimax.common.async.AsyncTaskService;
import com.minimax.common.cache.CacheService;
import com.minimax.common.ratelimit.RateLimitService;
import com.minimax.common.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 13 调优套件 — 单元测试
 */
class OptimizationTest {

    @Test
    void rateLimiterAllowsUpToCapacity() {
        RateLimiter rl = new RateLimiter(5, 5, 60);
        for (int i = 0; i < 5; i++) {
            assertTrue(rl.tryConsume("k"), "第 " + (i+1) + " 次应通过");
        }
        assertFalse(rl.tryConsume("k"), "第 6 次应被限流");
    }

    @Test
    void rateLimiterIsolatesKeys() {
        RateLimiter rl = new RateLimiter(2, 2, 60);
        assertTrue(rl.tryConsume("a"));
        assertTrue(rl.tryConsume("a"));
        assertFalse(rl.tryConsume("a"));
        // b 独立
        assertTrue(rl.tryConsume("b"));
        assertTrue(rl.tryConsume("b"));
        assertFalse(rl.tryConsume("b"));
    }

    @Test
    void rateLimiterTryConsumeMany() {
        RateLimiter rl = new RateLimiter(10, 10, 60);
        assertTrue(rl.tryConsume("k", 5));
        assertTrue(rl.tryConsume("k", 5));
        assertFalse(rl.tryConsume("k", 1));
    }

    @Test
    void rateLimitServiceMultiScope() {
        RateLimitService svc = new RateLimitService();
        assertTrue(svc.tryAcquireIp("1.2.3.4"));
        assertTrue(svc.tryAcquireUser(1L));
        assertTrue(svc.tryAcquireUser(2L));
        assertTrue(svc.tryAcquireGlobal());
    }

    @Test
    void cacheBasic() {
        CacheService c = new CacheService();
        c.define("test", 100, Duration.ofMinutes(1));
        c.put("test", "k1", "v1");
        assertEquals("v1", c.get("test", "k1"));
        assertEquals(1, c.size("test"));
        c.invalidate("test", "k1");
        assertNull(c.get("test", "k1"));
    }

    @Test
    void cacheGetOrLoad() {
        CacheService c = new CacheService();
        c.define("test", 100, Duration.ofMinutes(1));
        AtomicInteger calls = new AtomicInteger(0);
        // 第一次: 调 loader
        String v1 = c.getOrLoad("test", "x", k -> {
            calls.incrementAndGet();
            return "computed";
        });
        // 第二次: 命中缓存, 不调 loader
        String v2 = c.getOrLoad("test", "x", k -> {
            calls.incrementAndGet();
            return "should-not-run";
        });
        assertEquals("computed", v1);
        assertEquals("computed", v2);
        assertEquals(1, calls.get(), "loader should only run once");
    }

    @Test
    void cacheStats() {
        CacheService c = new CacheService();
        c.define("test", 100, Duration.ofMinutes(1));
        c.put("test", "k1", "v1");
        c.get("test", "k1");  // hit
        c.get("test", "missing");  // miss
        var stats = c.stats("test");
        assertNotNull(stats);
        assertTrue(stats.hitCount() >= 1);
        assertTrue(stats.missCount() >= 1);
    }

    @Test
    void asyncTaskSubmitAndStatus() throws Exception {
        AsyncTaskService svc = new AsyncTaskService(2, 4, 10, 3);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger(0);

        String id = svc.submit("test", () -> {
            ran.incrementAndGet();
            latch.countDown();
        });
        assertNotNull(id);
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
        // 等待完成
        Thread.sleep(100);
        AsyncTaskService.TaskStatus s = svc.status(id);
        assertNotNull(s);
        assertEquals("done", s.status);
        assertEquals(1, ran.get());
    }

    @Test
    void asyncTaskFailureRetry() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        AsyncTaskService svc = new AsyncTaskService(1, 1, 5, 3);
        String id = svc.submit("failtest", () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("boom");
        });
        Thread.sleep(500);
        AsyncTaskService.TaskStatus s = svc.status(id);
        assertEquals("failed", s.status);
        assertEquals(3, attempts.get(), "should retry 3 times");
    }

    @Test
    void asyncTaskWithResult() throws Exception {
        AsyncTaskService svc = new AsyncTaskService(1, 1, 5, 1);
        String id = svc.submit("withresult", () -> 42, new AsyncTaskService.AsyncResultHandler<Integer>() {
            @Override public void onSuccess(Integer r) { assertEquals(42, r); }
            @Override public void onFailure(Exception e) { fail("should not fail"); }
        });
        Thread.sleep(200);
        assertEquals("done", svc.status(id).status);
        assertEquals("42", svc.status(id).result);
    }

    @Test
    void asyncTaskFuture() throws Exception {
        AsyncTaskService svc = new AsyncTaskService(1, 1, 5, 1);
        var f = svc.submitFuture(() -> "hello");
        assertEquals("hello", f.get());
    }
}
