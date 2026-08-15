package com.minimax.ai.cluster;

import com.minimax.ai.entity.ClusterNode;
import com.minimax.ai.mapper.ClusterNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集群主节点选举 (V3.3.0)
 *
 * <p>简化版选举:
 *   1. 启动时, 如果无 leader, 自己是 ACTIVE 且 ID 最小的 → 选为 leader
 *   2. leader 离线后, 下一个候选升为 leader
 *   3. 5s 重新检测
 *
 * <h3>更复杂的方案</h3>
 * 真实生产用 Raft / Paxos / etcd. 这里简化为"最小 ID 优先"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeElection {

    private final ClusterNodeMapper mapper;
    private final NodeRegistry registry;

    /** 是否是 leader (本节点) */
    private final AtomicBoolean isLeader = new AtomicBoolean(false);

    /**
     * 5s 选举一次
     */
    @Scheduled(fixedRate = 5_000)
    public void elect() {
        try {
            // 1. 当前 leader
            ClusterNode current = mapper.findCurrentLeader();
            // 2. 如果 leader 仍在线, 跳过
            if (current != null && "ACTIVE".equals(current.getStatus())) {
                // 验证本节点状态
                boolean me = registry.currentNodeId().equals(current.getNodeId());
                isLeader.set(me);
                return;
            }
            // 3. 没 leader 或已离线, 选新
            List<ClusterNode> active = mapper.findActive();
            if (active.isEmpty()) {
                isLeader.set(false);
                return;
            }
            // 4. 选 ID 最小 (稳定 + 可预测)
            ClusterNode winner = active.stream()
                    .min((a, b) -> a.getNodeId().compareTo(b.getNodeId()))
                    .orElse(null);
            if (winner == null) return;
            // 5. 设 leader (数据库 set)
            mapper.setLeader(winner.getNodeId());
            // 6. 标记本节点
            boolean me = registry.currentNodeId().equals(winner.getNodeId());
            isLeader.set(me);
            if (me) {
                log.info("[cluster] 🎉 本节点选举为 LEADER: {}", winner.getNodeId());
            } else {
                log.info("[cluster] 新 leader: {} (本节点: {})", winner.getNodeId(), registry.currentNodeId());
            }
        } catch (Exception e) {
            log.debug("[cluster] 选举失败: {}", e.getMessage());
        }
    }

    public boolean isLeader() { return isLeader.get(); }
    public ClusterNode currentLeader() { return mapper.findCurrentLeader(); }
}
