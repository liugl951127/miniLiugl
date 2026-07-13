package com.minimax.ai.cluster.raft;

/**
 * Raft 节点状态 (V3.5.0 自研分布式一致性)
 *
 * <h3>3 种角色</h3>
 * <ul>
 *   <li><b>FOLLOWER</b>  跟随者: 接收 Leader 心跳和日志, 默认状态</li>
 *   <li><b>CANDIDATE</b> 候选者: 选举中, 拉票</li>
 *   <li><b>LEADER</b>    领导者: 处理写请求, 复制日志到 Follower</li>
 * </ul>
 */
public enum NodeState {
    FOLLOWER,
    CANDIDATE,
    LEADER
}
