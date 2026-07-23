package com.minimax.pipeline.function_ext.executor;

import java.util.Map;

/**
 * 工具函数接口。
 * 内置/自定义工具都实现此接口。
 */
public interface ToolFunction {
    /** 工具名 (与 FunctionTool.name 一致) */
    String name();

    /**
     * 执行工具。
     * @param args 参数 (从 LLM tool_call.function.arguments 解析)
     * @return 结果文本 (会作为 role=tool message 回传给 LLM)
     * @throws Exception 失败抛异常，由 ToolExecutor 捕获并记录
     */
    String execute(Map<String, Object> args) throws Exception;
}
