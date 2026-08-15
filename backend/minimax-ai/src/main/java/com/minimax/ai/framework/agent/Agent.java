package com.minimax.ai.framework.agent;

import com.minimax.ai.framework.memory.MemoryItem;
import com.minimax.ai.framework.memory.MemoryStore;
import com.minimax.ai.framework.permission.Permission;
import com.minimax.ai.framework.permission.PermissionGate;
import com.minimax.ai.framework.tool.Tool;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.stage.Tokenizer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 基类 (V2.8.6)
 *
 * <h3>类 LangChain4j AgentExecutor / Spring AI ChatClient</h3>
 * <p>实现 ReAct 推理循环 (Reason + Act):</p>
 * <pre>
 *   loop max_steps:
 *     thought  = decide next action       # 推理: 下一步做什么
 *     action   = execute tool/thought     # 执行
 *     observe  = collect result           # 观察
 *     if final_answer: break
 * </pre>
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>工具调用 (Tool Calling) - 自动选择 + 执行</li>
 *   <li>记忆管理 (Memory) - 短期 (对话) + 长期 (用户偏好)</li>
 *   <li>权限门控 (Permission) - 敏感操作前弹窗请求</li>
 *   <li>推理循环 (Reasoning) - 多步思考</li>
 *   <li>上下文维护 (Context) - 工具结果累积</li>
 * </ul>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>不依赖外部 LLM, 用 Java 自研推理 (基于关键词/规则/embedding 相似度)</li>
 *   <li>工具可注册, 业务 Agent 通过继承复用</li>
 *   <li>每步可观测 (日志/trace/耗时)</li>
 *   <li>失败有降级 (fallback)</li>
 * </ul>
 */
@Slf4j
public abstract class Agent {

    /** Agent 名称 */
    protected final String name;
    /** Agent 描述 (供路由用) */
    protected final String description;
    /** 系统提示词 */
    protected final String systemPrompt;
    /** 最大推理步数 (防死循环) */
    protected final int maxSteps;
    /** 已注册工具 */
    protected final Map<String, Tool> tools = new ConcurrentHashMap<>();
    /** 必填权限 (执行前必须获得) */
    protected final List<Permission> requiredPermissions = new ArrayList<>();
    /** Agent 能力标签 (供路由) */
    protected final List<String> capabilities = new ArrayList<>();
    /** 依赖注入 */
    protected MemoryStore memoryStore;
    protected PermissionGate permissionGate;
    protected Tokenizer tokenizer;

    protected Agent(String name, String description, String systemPrompt, int maxSteps) {
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.maxSteps = maxSteps > 0 ? maxSteps : 5;
    }

    /**
     * 注入依赖 (Spring bean 注入失败时手动调用)
     */
    public Agent withDependencies(MemoryStore memoryStore, PermissionGate permissionGate, Tokenizer tokenizer) {
        this.memoryStore = memoryStore;
        this.permissionGate = permissionGate;
        this.tokenizer = tokenizer;
        return this;
    }

    /**
     * 注册工具
     */
    public Agent registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
        log.debug("[agent:{}] registered tool: {}", name, tool.getName());
        return this;
    }

    /**
     * 添加必填权限
     */
    public Agent requirePermission(Permission permission) {
        requiredPermissions.add(permission);
        return this;
    }

    /**
     * 添加能力标签
     */
    public Agent addCapability(String capability) {
        capabilities.add(capability);
        return this;
    }

    /**
     * 主执行方法 (ReAct 循环)
     */
    public AgentResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        log.info("[agent:{}] === START session={} userQuery='{}' ===",
                name, context.sessionId,
                context.userQuery != null && context.userQuery.length() > 30
                        ? context.userQuery.substring(0, 30) + "..." : context.userQuery);

        AgentResult result = new AgentResult();
        result.agentName = name;
        result.sessionId = context.sessionId;
        result.steps = new ArrayList<>();
        result.success = false;

        try {
            // 1. 权限检查 (前置)
            if (permissionGate != null && !requiredPermissions.isEmpty()) {
                PermissionGate.PermissionResult pr = permissionGate.checkAll(
                        requiredPermissions, context);
                if (!pr.granted) {
                    result.permissionDenied = true;
                    result.permissionRequest = pr.request;
                    result.finalAnswer = "需要您授权: " + pr.request;
                    result.errorMessage = "PERMISSION_DENIED";
                    log.warn("[agent:{}] permission denied: {}", name, pr.request);
                    result.totalCostMs = System.currentTimeMillis() - start;
                    return result;
                }
            }

            // 2. 加载短期记忆 (最近对话)
            if (memoryStore != null) {
                List<MemoryItem> history = memoryStore.recallShortTerm(
                        context.sessionId, PipelineConfig.MAX_HISTORY_TURNS);
                context.history = history;
                log.debug("[agent:{}] loaded {} history items", name, history.size());
            }

            // 3. ReAct 推理循环
            context.thoughts = new ArrayList<>();
            context.actions = new ArrayList<>();
            context.observations = new ArrayList<>();
            for (int step = 0; step < maxSteps; step++) {
                AgentStep agentStep = new AgentStep();
                agentStep.stepNumber = step + 1;
                agentStep.startTime = System.currentTimeMillis();

                try {
                    // 3a. 思考 (Reasoning)
                    String thought = think(context);
                    agentStep.thought = thought;
                    context.thoughts.add(thought);
                    log.info("[agent:{}] step {}/{} thought: {}", name, step + 1, maxSteps, thought);

                    // 3b. 决策 (Action)
                    ActionDecision decision = decide(context, thought);
                    agentStep.action = decision.action;
                    agentStep.actionInput = decision.input;
                    context.actions.add(decision);

                    if (decision.isFinalAnswer()) {
                        // 最终答案
                        result.finalAnswer = decision.finalAnswer;
                        agentStep.isFinal = true;
                        result.steps.add(agentStep);
                        log.info("[agent:{}] FINAL ANSWER: {}", name, decision.finalAnswer);
                        break;
                    }

                    // 3c. 执行 (Act)
                    String toolName = decision.action;
                    Tool tool = tools.get(toolName);
                    if (tool == null) {
                        agentStep.error = "Tool not found: " + toolName;
                        context.observations.add("Error: tool '" + toolName + "' not found");
                        log.warn("[agent:{}] tool not found: {}", name, toolName);
                    } else {
                        Map<String, Object> toolResult = tool.execute(context, decision.input);
                        agentStep.toolResult = toolResult;
                        String obs = formatObservation(toolName, toolResult);
                        context.observations.add(obs);
                        log.info("[agent:{}] tool {} → {}", name, toolName,
                                obs.length() > 80 ? obs.substring(0, 80) + "..." : obs);
                    }
                } catch (Exception e) {
                    log.error("[agent:{}] step {} failed", name, step + 1, e);
                    agentStep.error = e.getMessage();
                    context.observations.add("Error: " + e.getMessage());
                }

                agentStep.endTime = System.currentTimeMillis();
                agentStep.costMs = agentStep.endTime - agentStep.startTime;
                result.steps.add(agentStep);
            }

            // 4. 若未达 final_answer, 汇总生成
            if (result.finalAnswer == null) {
                result.finalAnswer = summarize(context);
            }

            // 5. 记忆存储 (短期 + 长期偏好)
            if (memoryStore != null) {
                memoryStore.remember(context.sessionId, MemoryItem.userMessage(context.userQuery));
                memoryStore.remember(context.sessionId, MemoryItem.agentMessage(result.finalAnswer));
                // 提取偏好 → 长期记忆
                extractAndStorePreferences(context, result);
            }

            result.success = true;
        } catch (Exception e) {
            log.error("[agent:{}] execution failed", name, e);
            result.errorMessage = e.getMessage();
            result.finalAnswer = "抱歉, 处理遇到问题: " + e.getMessage();
        }

        result.totalCostMs = System.currentTimeMillis() - start;
        log.info("[agent:{}] === END session={} success={} steps={} costMs={} ===",
                name, context.sessionId, result.success, result.steps.size(), result.totalCostMs);
        return result;
    }

    /**
     * 思考 (Reasoning)
     * 子类实现: 决定下一步该做什么
     */
    protected abstract String think(AgentContext context);

    /**
     * 决策 (Action)
     * 子类实现: 把 thought 转成具体 action
     */
    protected abstract ActionDecision decide(AgentContext context, String thought);

    /**
     * 汇总 (Summarize) - 当未达 final_answer 时
     * 默认实现: 把所有 observation 拼起来
     */
    protected String summarize(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("基于查询: ").append(context.userQuery).append("\n\n");
        for (int i = 0; i < context.observations.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(context.observations.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 提取并存储偏好到长期记忆
     * 子类可覆盖
     */
    protected void extractAndStorePreferences(AgentContext context, AgentResult result) {
        // 默认: 不提取
    }

    /**
     * 格式化观察结果
     */
    protected String formatObservation(String toolName, Map<String, Object> result) {
        if (result == null) return "(empty result)";
        return "[" + toolName + "] " + result.toString();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getCapabilities() { return capabilities; }
    public Map<String, Tool> getTools() { return tools; }
    public List<Permission> getRequiredPermissions() { return requiredPermissions; }

    // ========================================================
    // 内部类
    // ========================================================

    /** Agent 执行上下文 */
    @lombok.Data
    public static class AgentContext {
        public String sessionId;
        public Long userId;
        public String userQuery;
        public Map<String, Object> variables = new LinkedHashMap<>();
        public List<MemoryItem> history = new ArrayList<>();
        public List<String> thoughts = new ArrayList<>();
        public List<ActionDecision> actions = new ArrayList<>();
        public List<String> observations = new ArrayList<>();
        /** 用户位置 (lat, lng) - 由 LBS 提供 */
        public Double userLat;
        public Double userLng;
        public String userCity;
    }

    /** 行动决策 */
    @lombok.Data
    public static class ActionDecision {
        public String action;          // 工具名 or "FINAL_ANSWER"
        public Map<String, Object> input = new LinkedHashMap<>();
        public String finalAnswer;     // 若 action=FINAL_ANSWER, 这里是答案
        public boolean isFinalAnswer() { return "FINAL_ANSWER".equals(action); }
    }

    /** 单步执行 */
    @lombok.Data
    public static class AgentStep {
        public int stepNumber;
        public long startTime;
        public long endTime;
        public long costMs;
        public String thought;
        public String action;
        public Map<String, Object> actionInput;
        public Map<String, Object> toolResult;
        public String error;
        public boolean isFinal;
    }

    /** Agent 结果 */
    @lombok.Data
    public static class AgentResult {
        public String agentName;
        public String sessionId;
        public boolean success;
        public String finalAnswer;
        public List<AgentStep> steps;
        public long totalCostMs;
        public String errorMessage;
        public boolean permissionDenied;
        public String permissionRequest;
    }
}
