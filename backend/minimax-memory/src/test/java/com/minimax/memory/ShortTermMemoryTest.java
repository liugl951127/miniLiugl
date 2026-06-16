package com.minimax.memory;

import com.minimax.memory.shortterm.ShortTermMemory;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 短期记忆测试 - 用 mock Redis（不连真实 Redis），强制走本地 fallback。
 */
class ShortTermMemoryTest {

    /** 强制 use-redis=false → 走 Caffeine fallback */
    private ShortTermMemory newMemory() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        return new ShortTermMemory(redis, false);
    }

    @Test
    void appendAndRecent() {
        ShortTermMemory mem = newMemory();
        Long sid = 100L;
        mem.append(sid, "user", "你好");
        mem.append(sid, "assistant", "你好世界");
        List<Map<String, String>> r = mem.recent(sid, 10);
        assertEquals(2, r.size());
        assertEquals("user", r.get(0).get("role"));
        assertEquals("你好", r.get(0).get("content"));
        assertEquals("assistant", r.get(1).get("role"));
    }

    @Test
    void sizeAndClear() {
        ShortTermMemory mem = newMemory();
        Long sid = 200L;
        assertEquals(0, mem.size(sid));
        mem.append(sid, "user", "a");
        mem.append(sid, "user", "b");
        assertEquals(2, mem.size(sid));
        mem.clear(sid);
        assertEquals(0, mem.size(sid));
    }

    @Test
    void recentLimit() {
        ShortTermMemory mem = newMemory();
        Long sid = 300L;
        for (int i = 0; i < 20; i++) {
            mem.append(sid, "user", "msg-" + i);
        }
        List<Map<String, String>> r = mem.recent(sid, 5);
        assertEquals(5, r.size());
        assertEquals("msg-15", r.get(0).get("content"));
        assertEquals("msg-19", r.get(4).get("content"));
    }

    @Test
    void isolatedSessions() {
        ShortTermMemory mem = newMemory();
        mem.append(1L, "user", "A");
        mem.append(2L, "user", "B");
        assertEquals(1, mem.size(1L));
        assertEquals(1, mem.size(2L));
    }
}
