package com.minimax.ai;

import com.minimax.ai.generation.WorkflowEngine;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowEngine 测试 (V2.7.3)
 */
class WorkflowEngineTest {

    @Test
    void testEmptyWorkflow() {
        WorkflowEngine engine = new WorkflowEngine(null);
        WorkflowEngine.Workflow wf = new WorkflowEngine.Workflow("empty");
        WorkflowEngine.WorkflowResult r = engine.execute(wf, Map.of());
        assertTrue(r.success);
        assertEquals(0, r.outputs.size());
    }

    @Test
    void testSingleNode() {
        WorkflowEngine engine = new WorkflowEngine(null);
        WorkflowEngine.Workflow wf = new WorkflowEngine.Workflow("test");
        wf.addNode("n1", "sql.query", Map.of("dataSourceId", 1, "question", "test"));
        // 没有 tool registry 实际能执行, 但单节点流程结构本身能处理
        WorkflowEngine.WorkflowResult r = engine.execute(wf, Map.of());
        // 因为 toolRegistry 是 null, 会失败, 但工作流结构是 OK 的
        assertNotNull(r);
        assertEquals(1, r.nodes.size());
    }

    @Test
    void testTopoSortLinear() throws Exception {
        WorkflowEngine engine = new WorkflowEngine(null);
        Method topoSort = WorkflowEngine.class.getDeclaredMethod("topoSort", WorkflowEngine.Workflow.class);
        topoSort.setAccessible(true);

        WorkflowEngine.Workflow wf = new WorkflowEngine.Workflow("linear");
        wf.addNode("a", "t", Map.of());
        wf.addNode("b", "t", Map.of());
        wf.addNode("c", "t", Map.of());
        wf.addEdge("a", "b");
        wf.addEdge("b", "c");

        @SuppressWarnings("unchecked")
        List<WorkflowEngine.Node> order = (List<WorkflowEngine.Node>) topoSort.invoke(engine, wf);
        assertEquals(3, order.size());
        assertEquals("a", order.get(0).id);
        assertEquals("b", order.get(1).id);
        assertEquals("c", order.get(2).id);
    }

    @Test
    void testTopoSortDiamond() throws Exception {
        WorkflowEngine engine = new WorkflowEngine(null);
        Method topoSort = WorkflowEngine.class.getDeclaredMethod("topoSort", WorkflowEngine.Workflow.class);
        topoSort.setAccessible(true);

        WorkflowEngine.Workflow wf = new WorkflowEngine.Workflow("diamond");
        wf.addNode("a", "t", Map.of());
        wf.addNode("b", "t", Map.of());
        wf.addNode("c", "t", Map.of());
        wf.addNode("d", "t", Map.of());
        wf.addEdge("a", "b");
        wf.addEdge("a", "c");
        wf.addEdge("b", "d");
        wf.addEdge("c", "d");

        @SuppressWarnings("unchecked")
        List<WorkflowEngine.Node> order = (List<WorkflowEngine.Node>) topoSort.invoke(engine, wf);
        assertEquals(4, order.size());
        assertEquals("a", order.get(0).id);  // 第一个必须是 a
        assertEquals("d", order.get(3).id);  // 最后一个必须是 d
    }

    @Test
    void testTopoSortWithCycle() throws Exception {
        WorkflowEngine engine = new WorkflowEngine(null);
        Method topoSort = WorkflowEngine.class.getDeclaredMethod("topoSort", WorkflowEngine.Workflow.class);
        topoSort.setAccessible(true);

        WorkflowEngine.Workflow wf = new WorkflowEngine.Workflow("cycle");
        wf.addNode("a", "t", Map.of());
        wf.addNode("b", "t", Map.of());
        wf.addEdge("a", "b");
        wf.addEdge("b", "a");  // 环

        Object result = topoSort.invoke(engine, wf);
        assertNull(result, "环应该返回 null");
    }

    @Test
    void testWorkflowBuilder() {
        WorkflowEngine.Workflow wf = new WorkflowEngine.Workflow("test")
                .addNode("step1", "tool.a", Map.of("x", 1))
                .addNode("step2", "tool.b", Map.of("y", 2))
                .addEdge("step1", "step2");
        assertEquals("test", wf.name);
        assertEquals(2, wf.nodes.size());
        assertEquals(1, wf.edges.size());
        assertNotNull(wf.findNode("step1"));
        assertNotNull(wf.findNode("step2"));
        assertNull(wf.findNode("nonexistent"));
    }

    @Test
    void testNodeStatus() {
        WorkflowEngine.Node n = new WorkflowEngine.Node("n1", "tool", Map.of());
        assertEquals("PENDING", n.status);
        n.status = "RUNNING";
        assertEquals("RUNNING", n.status);
    }
}
