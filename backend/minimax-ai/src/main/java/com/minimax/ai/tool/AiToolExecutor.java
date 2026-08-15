package com.minimax.ai.tool;

import com.minimax.ai.entity.AiTool;
import java.util.Map;/**
 * AiToolExecutor (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * AI 工具 - AiToolExecutor.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 AiToolExecutor 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */


public interface AiToolExecutor {
    String getCode();
    Object execute(AiTool tool, Map<String, Object> input) throws Exception;
}
