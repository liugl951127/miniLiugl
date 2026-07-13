package com.minimax.ai.raft;

/**
 * Raft 节点角色 (V3.5.0)
 *
 * <p>Raft 算法中节点 3 状态:
 * <ul>
 *   <li><b>FOLLOWER</b> 默认状态, 接收 leader RPC</li>
 *   <li><b>CANDIDATE</b> 选举中, 已投自己, 拉票</li>
 *   <li><b>LEADER</b> 已选出, 定期心跳 + 复制日志</li>
 * </ul>
 *
 * <p>状态转换:
 * <pre>
 *                       election timeout
 * FOLLOWER  ────────────────────────────▶  CANDIDATE
 *     ▲                                         │
 *     │ 发现更高 term 或 收到 leader 心跳        │ 获得多数票
 *     │                                         ▼
 *     └──────────────  LEADER  ◀──────────── CANDIDATE
 *                      │
 *                      │ 选举超时 (发现更高 term)
 *                      ▼
 *                  FOLLOWER
 * </pre>
 */
public enum RaftRole {
    FOLLOWER,
    CANDIDATE,
    LEADER
}
