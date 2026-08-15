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
 * SELECT 节点 (V5.32) - 列投影 + 重命名 + 计算列
 *
 * config: {
 *   columns: [
 *     { "alias": "user_id", "expr": "id" },         // 直接列
 *     { "alias": "full_name", "expr": "first_name + ' ' + last_name" },  // 计算
 *     { "alias": "amount_yuan", "expr": "amount / 100" }
 *   ]
 * }
 */
@Slf4j
@Component
public class SelectNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.SELECT; }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        List<Map<String, Object>> cols = (List<Map<String, Object>>) config.get("columns");
        if (cols == null || cols.isEmpty()) {
            throw new IllegalArgumentException("SELECT 缺 columns 配置");
        }
        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance();
        Expression[] compiled = new Expression[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            String expr = (String) cols.get(i).get("expr");
            compiled[i] = engine.compile(expr, true);
        }
        log.info("[{}] Select: {} columns", nodeId, cols.size());

        List<Map<String, Object>> rows = inputs.values().iterator().next();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> env = new HashMap<>(row);
            Map<String, Object> newRow = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                Map<String, Object> colDef = cols.get(i);
                String alias = (String) colDef.get("alias");
                if (alias == null) alias = "col_" + i;
                Object val;
                try {
                    val = compiled[i].execute(env);
                } catch (Exception e) {
                    throw new BizException("SELECT 列 '" + alias + "' 计算失败: " + e.getMessage());
                }
                newRow.put(alias, val);
            }
            out.add(newRow);
        }
        return out;
    }
}
