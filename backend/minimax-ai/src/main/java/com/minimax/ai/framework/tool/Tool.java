package com.minimax.ai.framework.tool;

import com.minimax.ai.framework.agent.Agent.AgentContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具抽象 (V2.8.6)
 *
 * <h3>类 LangChain4j ToolSpecification / Spring AI @Tool</h3>
 * <p>业务 Agent 可注册任意数量的 Tool, Agent 推理循环会自动选择 + 调用.</p>
 *
 * <h3>Tool 实现要求</h3>
 * <ul>
 *   <li>无副作用, 纯函数 (除显式声明的可写操作)</li>
 *   <li>输入/输出都用 Map (JSON-like)</li>
 *   <li>失败抛异常, Agent 会捕获并降级</li>
 * </ul>
 */
public interface Tool {

    /** 工具名 (唯一, 用于 Agent 调用) */
    String getName();

    /** 工具描述 (供 LLM 选择) */
    String getDescription();

    /** 输入参数 schema (可选, 供前端生成表单) */
    default Map<String, ParameterDef> getParameters() {
        return new LinkedHashMap<>();
    }

    /** 工具分类 */
    default String getCategory() {
        return "general";
    }

    /** 是否需要敏感操作权限 */
    default boolean requiresPermission() {
        return false;
    }

    /** 所需权限编码 (供 PermissionGate 决策) */
    default String requiredPermissionCode() {
        return null;
    }

    /**
     * 工具执行
     *
     * @param context Agent 上下文 (含 sessionId, userId, userLocation)
     * @param input   用户/LLM 提供的参数
     * @return 工具结果 (Map, JSON 序列化)
     */
    Map<String, Object> execute(AgentContext context, Map<String, Object> input) throws Exception;

    /** 参数定义 */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class ParameterDef {
        public String name;
        public String type;        // "string" / "number" / "boolean" / "object" / "array"
        public String description;
        public boolean required;
        public Object defaultValue;
    }
}
