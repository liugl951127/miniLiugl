package com.minimax.pipeline.executor;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 节点执行器抽象基类 (V5.32)
 *
 * 子类只需实现 doExecute(), 输入输出都是 List<Map<String, Object>> (一行一个 map)
 *
 * 例: FilterNode → 条件过滤
 *     JoinNode → 2 入度, hash join
 *     MysqlInputNode → 0 入度, JDBC 查表
 */
@Slf4j
public abstract class NodeExecutor {

    /** 声明支持的节点类型 (子类用 enum 重写) */
    public abstract NodeType supportedType();

    /**
     * 执行入口 (Template Method)
     *
     * @param nodeId  画布节点 id (用于 ctx.putOutput)
     * @param config  节点配置 JSON
     * @param inputs  key=upstreamNodeId, value=rows
     * @param ctx     共享执行上下文
     */
    public final List<Map<String, Object>> execute(String nodeId,
                                                    Map<String, Object> config,
                                                    Map<String, List<Map<String, Object>>> inputs,
                                                    ExecutionContext ctx) {
        validateInputs(inputs);
        try {
            List<Map<String, Object>> out = doExecute(nodeId, config, inputs, ctx);
            ctx.putOutput(nodeId, out == null ? List.of() : out);
            return out;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("节点 {} ({}) 执行失败: {}", nodeId, supportedType(), e.getMessage(), e);
            throw new BizException("节点 " + nodeId + " 执行失败: " + e.getMessage());
        }
    }

    /** 校验入度数量 */
    protected void validateInputs(Map<String, List<Map<String, Object>>> inputs) {
        int expected = supportedType().getInputArity();
        int actual = inputs == null ? 0 : inputs.size();
        if (expected == 0 && actual > 0) {
            throw new BizException(supportedType() + " 是 INPUT 节点, 不应接收上游数据 (实际 " + actual + " 个)");
        }
        if (expected == 1 && actual != 1) {
            throw new BizException(supportedType() + " 需要 1 个上游, 实际 " + actual);
        }
        if (expected == 2 && actual != 2) {
            throw new BizException(supportedType() + " 需要 2 个上游, 实际 " + actual);
        }
    }

    /** 子类实现 */
    protected abstract List<Map<String, Object>> doExecute(String nodeId,
                                                           Map<String, Object> config,
                                                           Map<String, List<Map<String, Object>>> inputs,
                                                           ExecutionContext ctx) throws Exception;
}
