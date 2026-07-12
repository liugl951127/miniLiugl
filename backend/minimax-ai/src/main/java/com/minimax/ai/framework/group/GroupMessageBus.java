package com.minimax.ai.framework.group;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 群组消息总线 (V3.0.3)
 *
 * <p>Agent 间通信通道, 支持:
 *   - 点对点 (from→to)
 *   - 广播 (from→null)
 *   - 历史回放
 *
 * <h3>使用模式</h3>
 * <pre>
 *   bus.send(GroupMessage.builder().from("manager").to("worker1").type(TASK).build());
 *   List&lt;GroupMessage&gt; inbox = bus.receive("worker1");
 * </pre>
 *
 * <h3>线程安全</h3>
 * 每个 Agent 维护一个 ConcurrentLinkedQueue 作为收件箱
 */
@Slf4j
public class GroupMessageBus {

    /** Agent 收件箱: agentName → 消息队列 */
    private final Map<String, ConcurrentLinkedQueue<GroupMessage>> inboxes = new ConcurrentHashMap<>();
    /** 全部历史 (用于回放/审计) */
    private final List<GroupMessage> history = new ArrayList<>();
    /** 历史锁 (List 不线程安全) */
    private final Object historyLock = new Object();

    /**
     * 注册 Agent 收件箱
     *
     * @param agentName Agent 名
     */
    public void register(String agentName) {
        // 1. 防御: 不覆盖已有收件箱
        inboxes.computeIfAbsent(agentName, k -> new ConcurrentLinkedQueue<>());
        log.debug("[bus] 注册 agent 收件箱: {}", agentName);
    }

    /**
     * 注销 Agent
     */
    public void unregister(String agentName) {
        inboxes.remove(agentName);
    }

    /**
     * 发送消息
     *
     * @param msg 消息
     */
    public void send(GroupMessage msg) {
        // 1. 设置默认 ID 和时间戳
        if (msg.getId() == null) msg.setId(UUID.randomUUID().toString());
        if (msg.getTimestamp() == 0) msg.setTimestamp(System.currentTimeMillis());
        // 2. 记录历史
        synchronized (historyLock) {
            history.add(msg);
        }
        // 3. 路由: 广播 or 点对点
        if (msg.getTo() == null || msg.getTo().isEmpty()) {
            // 3a. 广播: 推送到所有收件箱 (除发送者自己)
            for (Map.Entry<String, ConcurrentLinkedQueue<GroupMessage>> e : inboxes.entrySet()) {
                if (!e.getKey().equals(msg.getFrom())) {
                    e.getValue().offer(msg);
                }
            }
            log.debug("[bus] 广播: {} → ({} 个收件人), type={}", msg.getFrom(), inboxes.size() - 1, msg.getType());
        } else {
            // 3b. 点对点
            ConcurrentLinkedQueue<GroupMessage> inbox = inboxes.get(msg.getTo());
            if (inbox != null) {
                inbox.offer(msg);
                log.debug("[bus] 发送: {} → {}, type={}", msg.getFrom(), msg.getTo(), msg.getType());
            } else {
                log.warn("[bus] 收件人不存在: {} (msg from={})", msg.getTo(), msg.getFrom());
            }
        }
    }

    /**
     * 接收消息 (Agent 拉取)
     *
     * @param agentName Agent 名
     * @return 收件箱中所有消息 (FIFO)
     */
    public List<GroupMessage> receive(String agentName) {
        // 1. 取收件箱
        ConcurrentLinkedQueue<GroupMessage> inbox = inboxes.get(agentName);
        if (inbox == null) {
            // 2. 未注册 → 返回空
            return new ArrayList<>();
        }
        // 3. 取出所有消息
        List<GroupMessage> result = new ArrayList<>();
        GroupMessage msg;
        while ((msg = inbox.poll()) != null) {
            result.add(msg);
        }
        return result;
    }

    /**
     * 偷看 (不移除)
     */
    public List<GroupMessage> peek(String agentName) {
        ConcurrentLinkedQueue<GroupMessage> inbox = inboxes.get(agentName);
        if (inbox == null) return new ArrayList<>();
        return new ArrayList<>(inbox);
    }

    /**
     * 获取历史
     */
    public List<GroupMessage> getHistory() {
        synchronized (historyLock) {
            return new ArrayList<>(history);
        }
    }

    /**
     * 收件箱大小
     */
    public int inboxSize(String agentName) {
        ConcurrentLinkedQueue<GroupMessage> inbox = inboxes.get(agentName);
        return inbox == null ? 0 : inbox.size();
    }

    /**
     * 清空所有 (群组结束)
     */
    public void clear() {
        inboxes.clear();
        synchronized (historyLock) {
            history.clear();
        }
    }
}
