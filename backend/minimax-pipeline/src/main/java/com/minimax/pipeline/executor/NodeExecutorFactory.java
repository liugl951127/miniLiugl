package com.minimax.pipeline.executor;

import com.minimax.common.exception.BizException;
import com.minimax.pipeline.enums.NodeType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器工厂 (V5.32)
 *
 * 自动注册所有 NodeExecutor Bean, 按 NodeType 路由
 */
@Slf4j
@Component
public class NodeExecutorFactory {

    private final List<NodeExecutor> executors;
    private final Map<NodeType, NodeExecutor> registry = new EnumMap<>(NodeType.class);

    public NodeExecutorFactory() {
        this.executors = List.of();
    }

    /** Spring 注入用构造器 (V5.32) */
    public NodeExecutorFactory(List<NodeExecutor> executors) {
        this.executors = executors;
    }

    @PostConstruct
    public void init() {
        doInit(executors);
    }

    /** V5.32: 测试用 init, 接受显式 list */
    public void init(List<NodeExecutor> list) {
        doInit(list);
    }

    private void doInit(List<NodeExecutor> list) {
        for (NodeExecutor e : list) {
            NodeExecutor prev = registry.put(e.supportedType(), e);
            if (prev != null) {
                throw new IllegalStateException("NodeType " + e.supportedType() + " 有多个 Executor: " + prev + " / " + e);
            }
        }
        log.info("V5.32 NodeExecutor 注册: {} 个 (类型: {})", registry.size(), registry.keySet());
    }

    public NodeExecutor get(NodeType type) {
        NodeExecutor e = registry.get(type);
        if (e == null) throw new BizException("未注册的节点类型: " + type);
        return e;
    }

    public int size() { return registry.size(); }
}
