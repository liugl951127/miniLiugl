package com.minimax.ai.framework.group;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 群组共享内存 (V3.0.3)
 *
 * <p>黑板模式: 所有 Agent 共享一份 KV 状态, 可读可写
 * <p>线程安全: ConcurrentHashMap + ReadWriteLock
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>Worker 写入中间结果, Manager 读取聚合</li>
 *   <li>Critic 写入评审意见, Worker 读取修改</li>
 *   <li>所有 Agent 共享任务上下文 (e.g. 原始用户输入)</li>
 * </ul>
 *
 * <h3>键约定</h3>
 * <ul>
 *   <li>task.{taskId} → 任务详情</li>
 *   <li>result.{taskId} → 任务结果</li>
 *   <li>consensus → 最终共识</li>
 *   <li>history → 决策历史 (List)</li>
 * </ul>
 */
public class GroupSharedMemory {

    /** 共享 KV (线程安全) */
    private final Map<String, Object> store = new ConcurrentHashMap<>();
    /** 读写锁 (复杂场景用) */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 写入 (覆盖)
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, Object value) {
        // 1. 写锁 (保护复合操作)
        lock.writeLock().lock();
        try {
            // 2. 存入
            store.put(key, value);
        } finally {
            // 3. 释放锁
            lock.writeLock().unlock();
        }
    }

    /**
     * 读取 (可能 null)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        // 1. 读锁 (并发读)
        lock.readLock().lock();
        try {
            // 2. 取值
            return (T) store.get(key);
        } finally {
            // 3. 释放锁
            lock.readLock().unlock();
        }
    }

    /**
     * 读取 (带默认值)
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        lock.readLock().lock();
        try {
            return (T) store.getOrDefault(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 累加 (用于投票计数等)
     *
     * @param key   键
     * @param delta 增量
     * @return 累加后值
     */
    public int increment(String key, int delta) {
        lock.writeLock().lock();
        try {
            // 1. 取当前值
            int current = 0;
            Object v = store.get(key);
            if (v instanceof Integer) {
                current = (Integer) v;
            }
            // 2. 累加
            int next = current + delta;
            store.put(key, next);
            // 3. 返回新值
            return next;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 追加到列表 (用于历史记录)
     */
    @SuppressWarnings("unchecked")
    public void appendToList(String key, Object item) {
        lock.writeLock().lock();
        try {
            // 1. 取现有列表
            List<Object> list = (List<Object>) store.get(key);
            if (list == null) {
                list = new ArrayList<>();
            }
            // 2. 追加
            list.add(item);
            // 3. 存回
            store.put(key, list);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 列出所有键
     */
    public Set<String> keys() {
        lock.readLock().lock();
        try {
            return new HashSet<>(store.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            store.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 快照 (用于持久化 / 调试)
     */
    public Map<String, Object> snapshot() {
        lock.readLock().lock();
        try {
            // 防御性拷贝, 防止外部修改
            return new HashMap<>(store);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 大小
     */
    public int size() {
        return store.size();
    }
}
