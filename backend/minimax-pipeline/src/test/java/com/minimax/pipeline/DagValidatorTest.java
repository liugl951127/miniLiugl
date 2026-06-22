package com.minimax.pipeline;

import com.minimax.pipeline.dto.WorkflowDto;
import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.service.DagValidator;
import com.minimax.pipeline.vo.DagValidationVo;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAG 校验器测试 (V5.32) - Kahn 拓扑排序
 */
class DagValidatorTest {

    @Test void linearDag_TopoOrder() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n2", NodeType.FILTER, null),
                node("n3", NodeType.REPORT_OUTPUT, null)
        ), List.of("n1->n2", "n2->n3"));
        DagValidationVo vo = DagValidator.validate(dto);
        assertTrue(vo.isValid());
        assertEquals(3, vo.getExecutionOrder().size());
        assertEquals(List.of("n1"), vo.getExecutionOrder().get(0));
        assertEquals(List.of("n2"), vo.getExecutionOrder().get(1));
        assertEquals(List.of("n3"), vo.getExecutionOrder().get(2));
    }

    @Test void parallelDag_SameLevel() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n2", NodeType.MYSQL_INPUT, null),
                node("n3", NodeType.JOIN, null),
                node("n4", NodeType.REPORT_OUTPUT, null)
        ), List.of("n1->n3", "n2->n3", "n3->n4"));
        DagValidationVo vo = DagValidator.validate(dto);
        assertTrue(vo.isValid());
        // layer 0: [n1, n2], layer 1: [n3], layer 2: [n4]
        assertEquals(2, vo.getExecutionOrder().get(0).size());
        assertTrue(vo.getExecutionOrder().get(0).containsAll(List.of("n1", "n2")));
    }

    @Test void cycle_Invalid() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n2", NodeType.FILTER, null)
        ), List.of("n1->n2", "n2->n1"));
        DagValidationVo vo = DagValidator.validate(dto);
        assertFalse(vo.isValid());
        assertTrue(vo.getErrors().stream().anyMatch(e -> e.contains("环")));
    }

    @Test void missingNodeOnEdge() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n2", NodeType.FILTER, null)
        ), List.of("n1->n99", "n99->n2"));
        DagValidationVo vo = DagValidator.validate(dto);
        assertFalse(vo.isValid());
        assertTrue(vo.getErrors().stream().anyMatch(e -> e.contains("n99")));
    }

    @Test void inputNodeWithInbound_Errors() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n2", NodeType.FILTER, null)
        ), List.of("n2->n1"));  // INPUT 不应接收上游
        DagValidationVo vo = DagValidator.validate(dto);
        assertFalse(vo.isValid());
    }

    @Test void disconnectedNode_Warning() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n2", NodeType.FILTER, null),
                node("n3", NodeType.LIMIT, null)  // 孤立
        ), List.of("n1->n2"));
        DagValidationVo vo = DagValidator.validate(dto);
        assertTrue(vo.getWarnings().stream().anyMatch(w -> w.contains("n3")));
    }

    @Test void duplicateNodeId() {
        WorkflowDto dto = makeWorkflow(List.of(
                node("n1", NodeType.MYSQL_INPUT, null),
                node("n1", NodeType.FILTER, null)
        ), List.of());
        DagValidationVo vo = DagValidator.validate(dto);
        assertFalse(vo.isValid());
    }

    @Test void emptyNodes() {
        WorkflowDto dto = new WorkflowDto();
        dto.setName("empty");
        dto.setNodes(List.of());
        dto.setEdges(List.of());
        DagValidationVo vo = DagValidator.validate(dto);
        assertFalse(vo.isValid());
    }

    @Test void quickValidateThrows() {
        WorkflowDto dto = new WorkflowDto();
        dto.setName("bad");
        dto.setNodes(List.of(node("n1", NodeType.MYSQL_INPUT, null), node("n2", NodeType.FILTER, null)));
        WorkflowDto.WorkflowEdge e1 = new WorkflowDto.WorkflowEdge();
        e1.setId("e1"); e1.setFrom("n1"); e1.setTo("n2");
        WorkflowDto.WorkflowEdge e2 = new WorkflowDto.WorkflowEdge();
        e2.setId("e2"); e2.setFrom("n2"); e2.setTo("n1");
        dto.setEdges(List.of(e1, e2));
        assertThrows(Exception.class, () -> DagValidator.quickValidate(dto));
    }

    // ---- helpers ----

    private WorkflowDto.WorkflowNode node(String id, NodeType type, Map<String, Object> cfg) {
        WorkflowDto.WorkflowNode n = new WorkflowDto.WorkflowNode();
        n.setId(id); n.setType(type); n.setName(id);
        n.setConfig(cfg);
        return n;
    }

    private WorkflowDto makeWorkflow(List<WorkflowDto.WorkflowNode> nodes, List<String> edgeStrs) {
        WorkflowDto dto = new WorkflowDto();
        dto.setName("test");
        dto.setNodes(new ArrayList<>(nodes));
        List<WorkflowDto.WorkflowEdge> edges = new ArrayList<>();
        int i = 0;
        for (String es : edgeStrs) {
            String[] parts = es.split("->");
            WorkflowDto.WorkflowEdge e = new WorkflowDto.WorkflowEdge();
            e.setId("e" + (i++));
            e.setFrom(parts[0]);
            e.setTo(parts[1]);
            edges.add(e);
        }
        dto.setEdges(edges);
        return dto;
    }
}
