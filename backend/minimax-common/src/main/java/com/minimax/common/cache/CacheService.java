package com.minimax.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 缓存服务 (Caffeine 封装).
 *
 * 支持多个命名 cache, 每个独立配置 TTL/容量.
 *
 * 用法:
 *   CacheService cache = new CacheService();
 *   cache.define("user", 1000, Duration.ofMinutes(5));
 *   cache.put("user", "u:1", userObj);
 *   User u = cache.get("user", "u:1", User.class);
 *   User u = cache.getOrLoad("user", "u:2", () -> userMapper.selectById(2L));
 */
public class CacheService {

    private final ConcurrentMap<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();

    /**
     * 定义一个命名 cache. 重复定义同名会被覆盖.
     */
    public void define(String name, long maxSize, Duration ttl) {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
        caches.put(name, cache);
    }

    public void put(String name, String key, Object value) {
        Cache<String, Object> c = caches.get(name);
        if (c != null) c.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, String key) {
        Cache<String, Object> c = caches.get(name);
        return c == null ? null : (T) c.getIfPresent(key);
    }

    /**
     * 拿不到时调 loader 加载, 自动回填.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String name, String key, Function<String, T> loader) {
        Cache<String, Object> c = caches.get(name);
        if (c == null) return loader.apply(key);
        return (T) c.get(key, k -> loader.apply(k));
    }

    public void invalidate(String name, String key) {
        Cache<String, Object> c = caches.get(name);
        if (c != null) c.invalidate(key);
    }

    public void clear(String name) {
        Cache<String, Object> c = caches.get(name);
        if (c != null) c.invalidateAll();
    }

    public void clearAll() {
        caches.values().forEach(Cache::invalidateAll);
    }

    public CacheStats stats(String name) {
        Cache<String, Object> c = caches.get(name);
        return c == null ? null : c.stats();
    }

    public long size(String name) {
        Cache<String, Object> c = caches.get(name);
        return c == null ? 0 : c.estimatedSize();
    }
}
