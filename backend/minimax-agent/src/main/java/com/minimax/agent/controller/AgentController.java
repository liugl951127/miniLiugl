package com.minimax.agent.controller;

import com.minimax.agent.entity.CollabSession;
import com.minimax.agent.entity.KgEntity;
import com.minimax.agent.entity.KgRelation;
import com.minimax.agent.entity.Plugin;
import com.minimax.agent.service.AgentService;
import com.minimax.agent.service.CollabDbService;
import com.minimax.agent.service.KnowledgeGraphService;
import com.minimax.agent.service.MultiAgentService;
import com.minimax.agent.service.PluginService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * V2 Agent 控制器
 *
 * Agent (ReAct):
 *   POST /agent/run                 自主任务
 *
 * 知识图谱:
 *   POST   /agent/kg/entities        创建/upsert 实体
 *   GET    /agent/kg/entities/{id}   实体详情
 *   GET    /agent/kg/entities/search 搜索
 *   DELETE /agent/kg/entities/{id}   删除
 *   POST   /agent/kg/relations       创建关系
 *   GET    /agent/kg/entities/{id}/neighbors      1 跳邻居
 *   GET    /agent/kg/entities/{id}/2hop          2 跳
 *   GET    /agent/kg/path?from=X&to=Y            最短路径
 *
 * 协作 (WebSocket):
 *   WS     /ws/collab/{sessionId}?userId=1
 *   POST   /agent/collab/sessions     创建协作会话
 *   POST   /agent/collab/{id}/join    加入
 *   POST   /agent/collab/{id}/close   关闭
 *
 * 插件市场:
 *   GET    /agent/plugins             列出
 *   POST   /agent/plugins             发布
 *   GET    /agent/plugins/{id}        详情
 *   POST   /agent/plugins/{id}/rate   评分
 *   POST   /agent/plugins/{id}/toggle 启停
 *   DELETE /agent/plugins/{id}        删除
 */
@Tag(name = "AI智能体")
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agent;
    private final KnowledgeGraphService kg;
    private final CollabDbService collab;
    private final PluginService plugin;
    private final MultiAgentService multiAgent;  // V5.30.7: 注入 MultiAgentService

    // ---------- Agent ----------

    @Operation(summary = "运行智能体任务 (同步)")
    @PostMapping("/run")
    public Result<AgentService.AgentResult> run(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return Result.ok(agent.run(userId, goal, tools));
    }

    /**
     * V5.16: 流式执行 (SSE) — 实时推送 Agent 思考/工具调用/最终答案.
     * 客户端用 EventSource('http://.../agent/run-stream') 接收.
     * 事件类型: start / tools / step-start / thought / tool-call / observation / final / done / error
     */
    @Operation(summary = "V5.16: 流式运行 Agent (SSE, 实时推送思考过程)")
    @PostMapping(value = "/run-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return agent.runStream(userId, goal, tools);
    }

    /**
     * V5.16: Plan 模式 — 把目标拆成 3-7 步骤, 用户确认后再执行.
     * 用于复杂任务, 避免 agent 跑偏.
     */
    @Operation(summary = "V5.16: Plan 模式 (LLM 拆解目标为有序步骤)")
    @PostMapping("/plan")
    public Result<List<String>> plan(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        String goal = (String) body.get("goal");
        List<String> steps = agent.plan(userId, goal);
        return Result.ok(steps);
    }

    /**
     * V5.16: 执行已确认的 Plan — 按步骤依次执行, 汇总结果.
     */
    @Operation(summary = "V5.16: 执行 Plan (按步骤串行执行子任务)")
    @PostMapping("/run-plan")
    public Result<AgentService.AgentResult> runPlan(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> planSteps = (List<String>) body.get("planSteps");
        return Result.ok(agent.runPlan(userId, goal, planSteps));
    }

    /**
     * V5.16: 集成 RAG 长期记忆 — 执行时先检索相关记忆, 拼入 system prompt.
     */
    @Operation(summary = "V5.16: Run with Memory (RAG 长期记忆召回)")
    @PostMapping("/run-with-memory")
    public Result<AgentService.AgentResult> runWithMemory(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        return Result.ok(agent.runWithMemory(userId, goal, tools, sessionId));
    }

    // ---------- V5.17: Multi-Agent 协作 ----------

    /**
     * V5.17: 同步多智能体协作 (Planner + Executor + Critic).
     * 默认 critic 最多 3 轮重试.
     */
    @Operation(summary = "V5.17: 多智能体协作 (Planner + Executor + Critic)")
    @PostMapping("/multi/run")
    public Result<MultiAgentService.MultiAgentResult> multiRun(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return Result.ok(multiAgent.run(userId, goal, tools));
    }

    /**
     * V5.17: 流式多智能体 (SSE, 实时推送每个角色决策).
     */
    @Operation(summary = "V5.17: 流式多智能体 (SSE 实时推送 3 角色协作过程)")
    @PostMapping(value = "/multi/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter multiStream(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return multiAgent.runStream(userId, goal, tools);
    }

    /**
     * V5.17: 单独调用 Planner (复用 multi-agent 的 planner prompt).
     */
    @Operation(summary = "V5.17: 单独 Planner (LLM 拆解目标)")
    @PostMapping("/multi/plan")
    public Result<List<String>> multiPlan(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        String goal = (String) body.get("goal");
        String feedback = (String) body.get("feedback");
        // 复用 AgentService.plan (它只读 planSteps, 没有 feedback)
        // 多智能体版: feedback 透传给 planner
        // 这里直接用 multiAgent 内部, 临时反射调用
        List<String> steps = multiAgent.planSteps(goal, feedback);
        return Result.ok(steps);
    }

    /**
     * V5.17: 单独调用 Critic (评估执行结果).
     */
    @Operation(summary = "V5.17: 单独 Critic (评估 plan+results)")
    @PostMapping("/multi/critic")
    public Result<Map<String, Object>> multiCritic(@RequestBody Map<String, Object> body) {
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> plan = (List<String>) body.get("plan");
        String results = (String) body.get("results");
        MultiAgentService.CriticEval eval = multiAgent.evaluate(goal, plan, results);
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("passed", eval.passed());
        resp.put("score", eval.score());
        resp.put("feedback", eval.feedback());
        resp.put("improvedAnswer", eval.improvedAnswer());
        return Result.ok(resp);
    }

    // ---------- 知识图谱 ----------

    @Operation(summary = "创建/更新实体")
    @PostMapping("/kg/entities")
    public Result<Long> upsertEntity(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long id = kg.upsertEntity(userId,
                (String) body.get("name"),
                (String) body.get("type"),
                (String) body.get("description"),
                (String) body.get("aliases"),
                body.get("importance") == null ? null : ((Number) body.get("importance")).intValue());
        return Result.ok(id);
    }

    @Operation(summary = "获取实体详情")
    @GetMapping("/kg/entities/{id}")
    public Result<KgEntity> getEntity(@PathVariable Long id, @RequestParam Long userId) {
        return Result.ok(kg.getEntity(id, userId));
    }

    @Operation(summary = "搜索实体")
    @GetMapping("/kg/entities/search")
    public Result<List<KgEntity>> searchEntities(@RequestParam Long userId,
                                                   @RequestParam String keyword,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(kg.searchEntities(userId, keyword, limit));
    }

    @Operation(summary = "删除实体")
    @DeleteMapping("/kg/entities/{id}")
    public Result<Boolean> deleteEntity(@PathVariable Long id, @RequestParam Long userId) {
        return Result.ok(kg.deleteEntity(id, userId));
    }

    @Operation(summary = "创建实体关系")
    @PostMapping("/kg/relations")
    public Result<Long> createRelation(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long fromId = ((Number) body.get("fromId")).longValue();
        Long toId = ((Number) body.get("toId")).longValue();
        String type = (String) body.get("type");
        String desc = (String) body.get("description");
        Double weight = body.get("weight") == null ? null : ((Number) body.get("weight")).doubleValue();
        return Result.ok(kg.createRelation(userId, fromId, toId, type, desc, weight));
    }

    @Operation(summary = "获取实体1跳邻居")
    @GetMapping("/kg/entities/{id}/neighbors")
    public Result<List<Map<String, Object>>> neighbors(@PathVariable Long id) {
        return Result.ok(kg.neighbors(id));
    }

    @Operation(summary = "获取实体2跳邻居")
    @GetMapping("/kg/entities/{id}/2hop")
    public Result<List<Map<String, Object>>> twoHop(@PathVariable Long id) {
        return Result.ok(kg.twoHopNeighbors(id));
    }

    @Operation(summary = "查询最短路径")
    @GetMapping("/kg/path")
    public Result<List<KgEntity>> shortestPath(@RequestParam Long userId,
                                                 @RequestParam Long from,
                                                 @RequestParam Long to) {
        return Result.ok(kg.shortestPath(userId, from, to));
    }

    // ---------- 协作 ----------

    @Operation(summary = "创建协作会话")
    @PostMapping("/collab/sessions")
    public Result<Long> createCollab(@RequestBody Map<String, Object> body) {
        Long ownerId = ((Number) body.get("ownerId")).longValue();
        Integer max = body.get("maxUsers") == null ? null : ((Number) body.get("maxUsers")).intValue();
        return Result.ok(collab.createSession(ownerId, (String) body.get("title"), max));
    }


    @Operation(summary = "加入协作会话")
    @PostMapping("/collab/{id}/join")
    public Result<Boolean> joinCollab(@PathVariable("id") Long collabId, @RequestParam Long userId) {
        return Result.ok(collab.joinSession(collabId, userId, "editor"));
    }

    @Operation(summary = "关闭协作会话")
    @PostMapping("/collab/{id}/close")
    public Result<Boolean> closeCollab(@PathVariable("id") Long collabId, @RequestParam Long userId) {
        return Result.ok(collab.closeSession(collabId, userId));
    }

    // ---------- 插件市场 ----------

    @Operation(summary = "列出插件")
    @GetMapping("/plugins")
    public Result<List<Plugin>> listPlugins(@RequestParam(required = false) String category) {
        return Result.ok(plugin.listAll(category));
    }

    @Operation(summary = "获取插件详情")
    @GetMapping("/plugins/{id}")
    public Result<Plugin> getPlugin(@PathVariable Long id) {
        return Result.ok(plugin.get(id));
    }

    @Operation(summary = "发布插件")
    @PostMapping("/plugins")
    public Result<Long> publishPlugin(@RequestParam Long ownerId, @RequestBody Map<String, Object> body) {
        Long id = plugin.publish(ownerId,
                (String) body.get("name"),
                (String) body.get("displayName"),
                (String) body.get("description"),
                (String) body.get("version"),
                (String) body.get("author"),
                (String) body.get("category"),
                (String) body.get("entry"),
                (String) body.get("pluginType"),
                (String) body.get("config"));
        return Result.ok(id);
    }

    @Operation(summary = "评分插件")
    @PostMapping("/plugins/{id}/rate")
    public Result<Boolean> ratePlugin(@PathVariable Long id, @RequestParam Double score) {
        return Result.ok(plugin.rate(id, score));
    }

    @Operation(summary = "启用/禁用插件")
    @PostMapping("/plugins/{id}/toggle")
    public Result<Boolean> togglePlugin(@PathVariable Long id, @RequestParam Boolean enabled) {
        return Result.ok(plugin.setEnabled(id, enabled));
    }

    @Operation(summary = "删除插件")
    @DeleteMapping("/plugins/{id}")
    public Result<Boolean> deletePlugin(@PathVariable Long id, @RequestParam Long ownerId) {
        return Result.ok(plugin.delete(id, ownerId));
    }
}