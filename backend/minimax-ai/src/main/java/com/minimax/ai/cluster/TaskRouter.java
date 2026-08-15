package com.minimax.ai.cluster;

import com.minimax.ai.entity.ClusterNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务路由器 (V3.3.0)
 *
 * <p>根据任务特性 + 节点负载选最合适的节点
 *
 * <h3>策略</h3>
 * <ul>
 *   <li>LEAST_LOAD  - 选负载最低的节点</li>
 *   <li>ROUND_ROBIN - 轮询</li>
 *   <li>CAPABILITY  - 按能力匹配</li>
 *   <li>CURRENT     - 本节点 (跳过路由)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRouter {

    private final NodeRegistry registry;
    /** 轮询计数器 (thread-safe) */
    private final AtomicInteger rrCounter = new AtomicInteger(0);

    public enum Strategy { LEAST_LOAD, ROUND_ROBIN, CAPABILITY, CURRENT }

    /**
     * 路由
     *
     * @param capability 所需能力 (e.g. "llm", "vlm")
     * @param strategy   策略
     * @return 目标节点
     */
    public ClusterNode route(String capability, Strategy strategy) {
        return switch (strategy) {
            case LEAST_LOAD -> selectLeastLoad(capability);
            case ROUND_ROBIN -> selectRoundRobin();
            case CAPABILITY -> registry.selectByCapability(capability);
            case CURRENT -> registry.getById(registry.currentNodeId());
        };
    }

    /**
     * 选负载最低节点
     *
     * <p>负载 = 0.5*CPU + 0.3*内存 + 0.2*GPU
     */
    private ClusterNode selectLeastLoad(String capability) {
        List<ClusterNode> candidates = registry.listActive();
        if (capability != null) {
            candidates = candidates.stream()
                    .filter(n -> n.getCapabilities() != null && n.getCapabilities().contains(capability))
                    .toList();
        }
        return candidates.stream()
                .min((a, b) -> Double.compare(loadScore(a), loadScore(b)))
                .orElse(null);
    }

    /**
     * 轮询
     */
    private ClusterNode selectRoundRobin() {
        List<ClusterNode> active = registry.listActive();
        if (active.isEmpty()) return null;
        int idx = Math.floorMod(rrCounter.getAndIncrement(), active.size());
        return active.get(idx);
    }

    /**
     * 负载评分 (越低越优先)
     */
    private double loadScore(ClusterNode n) {
        double cpu = n.getCpuUsage() == null ? 0 : n.getCpuUsage();
        double mem = n.getMemoryUsage() == null ? 0 : n.getMemoryUsage();
        double gpu = n.getGpuUsage() == null ? 0 : n.getGpuUsage();
        double tasks = n.getActiveTasks() == null ? 0 : n.getActiveTasks() / 10.0;  // 归一化
        return cpu * 0.4 + mem * 0.3 + gpu * 0.2 + tasks * 0.1;
    }

    /**
     * 报告任务开始 (更新 activeTasks)
     */
    public void onTaskStart(String nodeId) {
        try {
            ClusterNode n = registry.getById(nodeId);
            if (n != null && n.getActiveTasks() != null) {
                registry.invalidateCache();
            }
        } catch (Exception ignored) {}
    }
}
