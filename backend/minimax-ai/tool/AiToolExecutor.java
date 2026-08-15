package com.minimax.ai.tool;

import com.minimax.ai.entity.AiTool;

import java.util.Map;

/**
 * AI 工具接口 (V2.5 自研)
 *
 * 所有工具实现这个接口
 * 通过 Spring 容器自动注册
 */
public interface AiToolExecutor {

    /**
     * 工具编码 (与 ai_tool.code 一致)
     * 用于自动匹配
     */
    String getCode();

    /**
     * 执行工具
     *
     * @param tool 工具定义
     * @param input 输入参数
     * @return 输出结果
     */
    Object execute(AiTool tool, Map<String, Object> input) throws Exception;
}
