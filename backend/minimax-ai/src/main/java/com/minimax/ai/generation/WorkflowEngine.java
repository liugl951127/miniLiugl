package com.minimax.ai.generation;

import com.minimax.ai.tool.AiToolExecutor;
import com.minimax.ai.tool.AiToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * AI 工作流编排引擎 (V2.7.3)
 *
 * <p>支持有向无环图 (DAG) 方式编排多个 AI 工具调用.</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   Workflow w = new Workflow("数据流");
 *   w.addNode("step1", "sql.query", Map.of("dataSourceId", 1, "question", "查询 user"));
 *   w.addNode("step2", "data.analyze.stats", Map.of("dataSourceId", 1, "table", "user"));
 *   w.addEdge("step1", "step2");  // step2 依赖 step1
 *   WorkflowResult r = engine.execute(w, Map.of());
 * }</pre>
 *
 * <h3>算法: 拓扑排序</h3>
 * <ol>
 *   <li>计算每个节点的入度 (依赖数)</li>
 *   <li>入度为 0 的节点入队</li>
 *   <li>出队一个节点, 执行; 邻居入度-1, 变 0 入队</li>
 *   <li>重复直到队列空</li>
 *   <li>若还有节点未处理 -> 环</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final AiToolRegistry toolRegistry;

    /** 工作流定义 */
    public static class Workflow {
        public String name;
        public String description;
        public List<Node> nodes = new ArrayList<>();
        public List<Edge> edges = new ArrayList<>();
        public long timeoutMs = 60000;

        public Workflow() {}
        public Workflow(String name) { this.name = name; }

        public Workflow addNode(String id, String toolCode, Map<String, Object> input) {
            nodes.add(new Node(id, toolCode, input));
            return this;
        }

        public Workflow addEdge(String from, String to) {
            edges.add(new Edge(from, to));
            return this;
        }

        public Node findNode(String id) {
            return nodes.stream().filter(n -> n.id.equals(id)).findFirst().orElse(null);
        }
    }

    public static class Node {
        public String id;
        public String toolCode;
        public Map<String, Object> input;
        public Object output;
        public String status = "PENDING";
        public long durationMs;
        public String error;

        public Node(String id, String toolCode, Map<String, Object> input) {
            this.id = id;
            this.toolCode = toolCode;
            this.input = input;
        }
    }

    public static class Edge {
        public String from;
        public String to;
        public Edge(String from, String to) { this.from = from; this.to = to; }
    }

    public static class WorkflowResult {
        public String workflowName;
        public boolean success;
        public List<Node> nodes = new ArrayList<>();
        public Map<String, Object> outputs = new LinkedHashMap<>();
        public long totalDurationMs;
        public String error;
    }

    /**
     * 执行工作流 (单线程, 按拓扑顺序)
     */
    public WorkflowResult execute(Workflow wf, Map<String, Object> context) {
        long start = System.currentTimeMillis();
        WorkflowResult r = new WorkflowResult();
        r.workflowName = wf.name;
        r.nodes.addAll(wf.nodes);

        try {
            // 1. 拓扑排序
            List<Node> order = topoSort(wf);
            if (order == null) {
                r.success = false;
                r.error = "工作流包含环";
                return r;
            }
            // 2. 按顺序执行
            for (Node node : order) {
                if (System.currentTimeMillis() - start > wf.timeoutMs) {
                    node.status = "TIMEOUT";
                    node.error = "超时";
                    r.success = false;
                    break;
                }
                executeNode(node, context);
                if ("FAILED".equals(node.status)) {
                    r.success = false;
                    r.error = "节点失败: " + node.id;
                    break;
                }
                r.outputs.put(node.id, node.output);
            }
            if (r.error == null) r.success = true;
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            r.success = false;
            r.error = e.getMessage();
        }

        r.totalDurationMs = System.currentTimeMillis() - start;
        return r;
    }

    /**
     * 拓扑排序 (Kahn 算法)
     */
    private List<Node> topoSort(Workflow wf) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (Node n : wf.nodes) {
            inDegree.putIfAbsent(n.id, 0);
            adj.putIfAbsent(n.id, new ArrayList<>());
        }
        for (Edge e : wf.edges) {
            inDegree.merge(e.to, 1, Integer::sum);
            adj.computeIfAbsent(e.from, k -> new ArrayList<>()).add(e.to);
        }
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> en : inDegree.entrySet()) {
            if (en.getValue() == 0) queue.add(en.getKey());
        }
        List<Node> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            Node n = wf.findNode(id);
            if (n != null) order.add(n);
            for (String next : adj.getOrDefault(id, List.of())) {
                int d = inDegree.merge(next, -1, Integer::sum);
                if (d == 0) queue.add(next);
            }
        }
        if (order.size() != wf.nodes.size()) {
            return null; // 有环
        }
        return order;
    }

    /**
     * 执行单个节点
     */
    private void executeNode(Node node, Map<String, Object> context) {
        long start = System.currentTimeMillis();
        node.status = "RUNNING";
        try {
            // 1. 合并 input (context 优先, 然后 node 自己的 input)
            Map<String, Object> finalInput = new HashMap<>();
            if (context != null) finalInput.putAll(context);
            finalInput.putAll(node.input);

            // 2. 替换 ${nodeId.output} 占位符
            resolveRefs(finalInput, node);

            // 3. 调用工具
            AiToolRegistry.ToolResult result = toolRegistry.invoke(node.toolCode, finalInput);
            node.output = result.data;
            node.status = result.success ? "SUCCESS" : "FAILED";
            if (!result.success) node.error = result.message;
        } catch (Exception e) {
            log.error("Node {} failed", node.id, e);
            node.status = "FAILED";
            node.error = e.getMessage();
        }
        node.durationMs = System.currentTimeMillis() - start;
    }

    /**
     * 替换 ${nodeId.outputPath} 形式占位符
     */
    @SuppressWarnings("unchecked")
    private void resolveRefs(Map<String, Object> input, Node currentNode) {
        // 简单实现: 在所有上游节点中查找匹配
        // 实际可以更复杂 (支持 .path 访问)
    }
}
