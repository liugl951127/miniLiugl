// =============================================================
// MiniMax - RAG KB 关系推理服务 (V7.3)
//
// 复用 AgentKgController.shortestPath 的 BFS 思路, 在 RAG 抽取的
// kb_extracted_relation 上做 2-hop / 3-hop 推理.
//
// 端点: GET /api/v1/rag/kg/reason?src=X&tgt=Y[&kbId=Z][&maxHops=3][&maxPaths=10]
//
// 流程:
//   1) src → tgt 直接边 (1 hop)
//   2) src → mid → tgt (2 hop)
//   3) src → m1 → m2 → tgt (3 hop)
//   4) 全部路径按 totalWeight 降序, 截取 maxPaths
//
// 评分: totalWeight = ∑ edge.weight (越高越相关)
//
// @author general
// @since 2026-08-22
// =============================================================

package com.minimax.rag.kg;

import com.minimax.rag.kg.entity.KbExtractedRelation;
import com.minimax.rag.kg.mapper.KbExtractedRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelationReasoner {

    /** 单次推理最大路径数 (防爆) */
    private static final int DEFAULT_MAX_PATHS = 10;
    /** 默认最大 hop 数 */
    private static final int DEFAULT_MAX_HOPS = 3;

    private final KbExtractedRelationMapper relMapper;

    /**
     * BFS 推理: 找 src → tgt 之间的所有路径 (限定 maxHops).
     *
     * @param kbId    限定 KB (可空: 跨 KB 推理)
     * @param src     起点实体
     * @param tgt     终点实体
     * @param maxHops 最多跳数 (1-3, 默认 3)
     * @return ReasonResult
     */
    public ReasonResult reason(Long kbId, String src, String tgt, int maxHops, int maxPaths) {
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("src 不能为空");
        }
        if (tgt == null || tgt.isBlank()) {
            throw new IllegalArgumentException("tgt 不能为空");
        }
        if (Objects.equals(src, tgt)) {
            // 自环, 视为 0 hop
            Path p = new Path(List.of(src), List.of(), 0);
            return new ReasonResult(true, 0, 1, List.of(p));
        }
        int hops = Math.max(1, Math.min(maxHops <= 0 ? DEFAULT_MAX_HOPS : maxHops, 3));
        int cap = Math.max(1, Math.min(maxPaths <= 0 ? DEFAULT_MAX_PATHS : maxPaths, 50));

        // 加载 KB 内全部边 (中小规模足够; 后续大数据量再换 Neo4j)
        List<KbExtractedRelation> edges = loadEdges(kbId);
        if (edges.isEmpty()) {
            return new ReasonResult(false, hops, 0, List.of());
        }

        // 邻接表: entity -> List<(neighbor, rel, weight)>
        Map<String, List<Edge>> adj = new HashMap<>();
        for (KbExtractedRelation e : edges) {
            adj.computeIfAbsent(e.getSrcEntity(), k -> new ArrayList<>())
                    .add(new Edge(e.getSrcEntity(), e.getRel(), e.getTgtEntity(), e.getWeight()));
            // 无向图: 反向也加
            adj.computeIfAbsent(e.getTgtEntity(), k -> new ArrayList<>())
                    .add(new Edge(e.getTgtEntity(), e.getRel(), e.getSrcEntity(), e.getWeight()));
        }

        // BFS 找所有路径
        List<Path> allPaths = new ArrayList<>();
        Deque<Node> stack = new ArrayDeque<>();
        Set<String> visitedInPath = new HashSet<>();
        stack.push(new Node(src, new ArrayList<>(), new ArrayList<>(), 0, visitedInPath));
        visitedInPath.add(src);

        while (!stack.isEmpty()) {
            Node cur = stack.pop();
            // 找到目标
            if (Objects.equals(cur.entity, tgt)) {
                List<String> pathEntities = new ArrayList<>(cur.entities);
                pathEntities.add(cur.entity);
                int totalWeight = cur.edges.stream().mapToInt(Edge::weight).sum();
                allPaths.add(new Path(pathEntities, cur.edges, totalWeight));
                if (allPaths.size() >= cap * 4) break; // 上限保护
                continue;
            }
            // 已达到最大跳数
            if (cur.depth >= hops) continue;
            // 展开邻居
            List<Edge> neighbors = adj.getOrDefault(cur.entity, List.of());
            for (Edge e : neighbors) {
                if (cur.visitedInPath.contains(e.tgt)) continue; // 避免环
                List<String> nextEntities = new ArrayList<>(cur.entities);
                nextEntities.add(cur.entity);
                List<Edge> nextEdges = new ArrayList<>(cur.edges);
                nextEdges.add(e);
                Set<String> nextVisited = new HashSet<>(cur.visitedInPath);
                nextVisited.add(e.tgt);
                stack.push(new Node(e.tgt, nextEntities, nextEdges, cur.depth + 1, nextVisited));
            }
        }

        // 排序: 总权重高 → 短路径优先
        allPaths.sort((a, b) -> {
            int w = Integer.compare(b.totalWeight, a.totalWeight);
            if (w != 0) return w;
            return Integer.compare(a.hops(), b.hops());
        });

        List<Path> top = allPaths.stream().limit(cap).collect(Collectors.toList());
        boolean found = !top.isEmpty();
        int minHops = found ? top.stream().mapToInt(Path::hops).min().orElse(-1) : -1;
        log.info("[RelationReasoner] src={} tgt={} kbId={} found={} paths={} minHops={}",
                src, tgt, kbId, found, top.size(), minHops);
        return new ReasonResult(found, minHops, top.size(), top);
    }

    /**
     * 加载全部边 (kbId == null → 跨 KB).
     */
    private List<KbExtractedRelation> loadEdges(Long kbId) {
        if (kbId == null) {
            // 跨 KB: 暂不支持大规模, 这里简化返回空
            return List.of();
        }
        return relMapper.selectByKb(kbId);
    }

    // ==================== 内部数据结构 ====================

    private record Node(String entity,
                        List<String> entities,
                        List<Edge> edges,
                        int depth,
                        Set<String> visitedInPath) {}

    private record Edge(String src, String rel, String tgt, int weight) {}

    /** 一条路径 (实体序列 + 边序列 + 总权重) */
    public record Path(List<String> entities, List<Edge> edges, int totalWeight) {
        public int hops() {
            return Math.max(0, entities.size() - 1);
        }

        public List<String> rels() {
            return edges.stream().map(Edge::rel).collect(Collectors.toList());
        }
    }

    /** 推理结果 */
    public record ReasonResult(boolean found, int minHops, int pathCount, List<Path> paths) {}
}
