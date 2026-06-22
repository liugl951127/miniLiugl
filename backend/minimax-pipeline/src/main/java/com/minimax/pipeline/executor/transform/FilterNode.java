package com.minimax.pipeline.executor.transform;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.minimax.common.exception.BizException;
import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * FILTER 节点 (V5.32) - Aviator 表达式
 *
 * config: {
 *   condition: "age > 18 && status == 'ACTIVE'"   // Aviator 表达式
 * }
 *
 * 示例:
 *   age > 18
 *   status == "ACTIVE"
 *   amount > 1000 && category == "VIP"
 *   DATE(created_at) >= "2024-01-01"
 */
@Slf4j
@Component
public class FilterNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.FILTER; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        String expr = (String) config.get("condition");
        if (expr == null || expr.isBlank()) throw new IllegalArgumentException("FILTER 缺 condition");
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance();
        Expression compiled = engine.compile(expr, true);
        log.info("[{}] Filter: {}", nodeId, expr);

        List<Map<String, Object>> rows = inputs.values().iterator().next();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> env = new HashMap<>(row);  // 浅拷贝, 防止污染
            Object result;
            try {
                result = compiled.execute(env);
            } catch (Exception e) {
                throw new BizException("FILTER 表达式求值失败 (" + expr + "): " + e.getMessage());
            }
            if (Boolean.TRUE.equals(result)) out.add(row);
        }
        return out;
    }
}
