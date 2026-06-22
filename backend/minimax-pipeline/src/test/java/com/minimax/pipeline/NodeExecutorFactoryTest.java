package com.minimax.pipeline;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.*;
import com.minimax.pipeline.executor.input.*;
import com.minimax.pipeline.executor.output.*;
import com.minimax.pipeline.executor.transform.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeExecutorFactory 测试 (V5.32)
 */
class NodeExecutorFactoryTest {

    private List<NodeExecutor> allExecutors() {
        return List.of(
                new MysqlInputNode(null), new FileInputNode(), new ApiInputNode(),
                new FilterNode(), new SelectNode(), new JoinNode(), new AggregateNode(),
                new SortNode(), new LimitNode(), new UnionNode(), new DistinctNode(),
                new DbOutputNode(null), new FileOutputNode(), new ReportOutputNode()
        );
    }

    @Test void allNodeTypesRegistered() {
        NodeExecutorFactory factory = new NodeExecutorFactory();
        factory.init(allExecutors());
        assertEquals(14, factory.size());
    }

    @Test void getRegisteredExecutor() {
        NodeExecutorFactory factory = new NodeExecutorFactory();
        factory.init(allExecutors());
        assertEquals(NodeType.FILTER, factory.get(NodeType.FILTER).supportedType());
        assertEquals(NodeType.JOIN, factory.get(NodeType.JOIN).supportedType());
        assertEquals(NodeType.AGGREGATE, factory.get(NodeType.AGGREGATE).supportedType());
        assertEquals(NodeType.DB_OUTPUT, factory.get(NodeType.DB_OUTPUT).supportedType());
    }

    @Test void getUnknownExecutor_Throws() {
        NodeExecutorFactory empty = new NodeExecutorFactory();
        empty.init(List.of());
        assertThrows(Exception.class, () -> empty.get(NodeType.MYSQL_INPUT));
    }

    @Test void duplicateRegistration_Throws() {
        NodeExecutorFactory factory = new NodeExecutorFactory();
        assertThrows(Exception.class, () -> factory.init(List.of(new FilterNode(), new FilterNode())));
    }
}
