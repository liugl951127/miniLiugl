package com.minimax.ai.tool.builtin;

import com.minimax.ai.entity.AiTool;
import com.minimax.ai.tool.AiToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 抽象简易 AI 工具基类 (V2.8.3)
 * 子类只需实现 doExecute(input), 自动处理错误/计时/返回格式
 */
public abstract class AbstractSimpleTool implements AiToolExecutor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> result = doExecute(input);
            if (result == null) result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("costMs", System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("{} failed", getCode(), e);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("message", "执行失败: " + e.getMessage());
            r.put("costMs", System.currentTimeMillis() - start);
            return r;
        }
    }

    protected abstract Map<String, Object> doExecute(Map<String, Object> input) throws Exception;

    /** 工具名称 (默认用类名) */
    public String getName() { return getClass().getSimpleName().replace("Tool", ""); }

    /** 工具描述 */
    public String getDescription() { return "AI 工具"; }

    /** 工具分类 */
    public String getCategory() { return "general"; }
}
