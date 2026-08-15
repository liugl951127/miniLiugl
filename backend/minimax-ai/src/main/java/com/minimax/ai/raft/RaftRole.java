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
 *//**
 * RaftRole (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * Raft 共识 - RaftRole.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 RaftRole 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */

public enum RaftRole {
    FOLLOWER,
    CANDIDATE,
    LEADER
}
