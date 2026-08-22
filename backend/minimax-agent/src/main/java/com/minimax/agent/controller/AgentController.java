package com.minimax.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimax.agent.entity.AgentTask;
import com.minimax.agent.entity.CollabSession;
import com.minimax.agent.entity.KgEntity;
import com.minimax.agent.entity.KgRelation;
import com.minimax.agent.entity.Plugin;
import com.minimax.agent.mapper.AgentTaskMapper;
import com.minimax.agent.service.AgentService;
import com.minimax.agent.service.CollabDbService;
import com.minimax.agent.service.KnowledgeGraphService;
import com.minimax.agent.service.MultiAgentService;
import com.minimax.agent.service.PluginService;
import com.minimax.common.result.Result;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * V2 Agent 控制器 (V6.8.2 安全修复)
 *
 * 安全修复: 所有写操作接口均从 JWT token 解析 userId，不再信任请求体中的 userId。
 * 防止 IDOR: 用户不能操作用户 ID 不匹配的资源。
 */
@Tag(name = "AI智能体")
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agent;
    private final KnowledgeGraphService kg;
    private final CollabDbService collab;
    private final PluginService plugin;
    private final MultiAgentService multiAgent;
    private final AgentTaskMapper agentTaskMapper;

    // V6.8.1: 运行中任务追踪（taskId → userId，用于鉴权 stop）
    private final ConcurrentHashMap<String, Long> taskOwners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicBoolean> runningTasks = new ConcurrentHashMap<>();

    /**
     * V6.8.2: 从 JWT 解析 userId，兼容旧 body.get("userId") 传参（过渡期）。
     * @throws SecurityException 如果未登录且没有 body userId
     */
    private Long resolveUserId(@AuthenticationPrincipal AuthenticatedUser user, Map<String, Object> body) {
        if (user != null) return user.id();
        if (body != null && body.get("userId") != null) {
            return ((Number) body.get("userId")).longValue();
        }
        throw new SecurityException("需要登录");
    }

    /**
     * V6.8.2: 从 @RequestParam 解析 userId（用于 GET 方法）
     */
    private Long resolveUserIdParam(@AuthenticationPrincipal AuthenticatedUser user, Long paramUserId) {
        if (user != null) return user.id();
        if (paramUserId != null) return paramUserId;
        throw new SecurityException("需要登录");
    }

    // ---------- Agent ----------

    @Operation(summary = "V6.8.1: 停止运行中的 Agent 任务（需本人）")
    @PostMapping("/stop")
    public Result<String> stop(@RequestBody Map<String, Object> body,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        String taskId = String.valueOf(body.get("id"));
        Long ownerId = taskOwners.get(taskId);
        // V6.8.2: 校验是否为任务发起者
        if (ownerId != null && user != null && !ownerId.equals(user.id())) {
            return Result.fail(403, "无权停止他人的任务");
        }
        AtomicBoolean flag = runningTasks.get(taskId);
        if (flag != null) {
            flag.set(true);
            runningTasks.remove(taskId);
            taskOwners.remove(taskId);
            log.info("[Agent] 任务 {} 已停止", taskId);
            return Result.ok("任务已停止");
        }
        return Result.ok("任务不存在或已完成");
    }

    @Operation(summary = "运行智能体任务 (同步)")
    @PostMapping("/run")
    public Result<AgentService.AgentResult> run(@RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        taskOwners.put(taskId, userId);
        runningTasks.put(taskId, new AtomicBoolean(false));
        try {
            Object goalObj = body.get("goal");
            String goal = goalObj instanceof String ? (String) goalObj : (goalObj != null ? goalObj.toString() : "");
            @SuppressWarnings("unchecked")
            Object toolsObj = body.get("tools");
            List<String> tools = toolsObj instanceof List ? (List<String>) toolsObj : List.of();
            if (Boolean.TRUE.equals(runningTasks.get(taskId).get())) {
                return Result.ok(AgentService.AgentResult.fail("任务已手动停止", List.of(), 0, Set.of()));
            }
            return Result.ok(agent.run(userId, goal, tools));
        } finally {
            runningTasks.remove(taskId);
            taskOwners.remove(taskId);
        }
    }

    @Operation(summary = "V5.16: 流式运行 Agent (SSE)")
    @PostMapping(value = "/run-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@RequestBody Map<String, Object> body,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        taskOwners.put(taskId, userId);
        runningTasks.put(taskId, new AtomicBoolean(false));
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return agent.runStream(userId, goal, tools);
    }

    @Operation(summary = "V5.16: Plan 模式 (LLM 拆解目标)")
    @PostMapping("/plan")
    public Result<List<String>> plan(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String goal = (String) body.get("goal");
        return Result.ok(agent.plan(userId, goal));
    }

    @Operation(summary = "V5.16: 执行 Plan (按步骤串行执行)")
    @PostMapping("/run-plan")
    public Result<AgentService.AgentResult> runPlan(@RequestBody Map<String, Object> body,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> planSteps = (List<String>) body.get("planSteps");
        return Result.ok(agent.runPlan(userId, goal, planSteps));
    }

    @Operation(summary = "V5.16: Run with Memory (RAG 长期记忆)")
    @PostMapping("/run-with-memory")
    public Result<AgentService.AgentResult> runWithMemory(@RequestBody Map<String, Object> body,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        return Result.ok(agent.runWithMemory(userId, goal, tools, sessionId));
    }

    // ---------- V5.17: Multi-Agent ----------

    @Operation(summary = "V5.17: 多智能体协作 (Planner + Executor + Critic)")
    @PostMapping("/multi/run")
    public Result<MultiAgentService.MultiAgentResult> multiRun(@RequestBody Map<String, Object> body,
                                                               @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return Result.ok(multiAgent.run(userId, goal, tools));
    }

    @Operation(summary = "V5.17: 流式多智能体 (SSE)")
    @PostMapping(value = "/multi/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter multiStream(@RequestBody Map<String, Object> body,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        String goal = (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) body.get("tools");
        return multiAgent.runStream(userId, goal, tools);
    }

    @Operation(summary = "V5.17: 单独 Planner")
    @PostMapping("/multi/plan")
    public Result<List<String>> multiPlan(@RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        resolveUserId(user, body);  // 鉴权，但不传 userId 给 planner
        String goal = (String) body.get("goal");
        String feedback = (String) body.get("feedback");
        List<String> steps = multiAgent.planSteps(goal, feedback);
        return Result.ok(steps);
    }

    @Operation(summary = "V5.17: 单独 Critic")
    @PostMapping("/multi/critic")
    public Result<Map<String, Object>> multiCritic(@RequestBody Map<String, Object> body,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        resolveUserId(user, body);  // 鉴权，但不传给 critic
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

    @Operation(summary = "V6.8.2: 多智能体历史记录")
    @GetMapping("/multi/history")
    public Result<List<AgentTask>> multiHistory(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestParam(defaultValue = "5") int limit) {
        Long userId = user != null ? user.getId() : null;
        QueryWrapper<AgentTask> qw = new QueryWrapper<>();
        if (userId != null) {
            qw.eq("user_id", userId);
        }
        qw.orderByDesc("created_at");
        qw.last("LIMIT " + Math.min(limit, 50));
        return Result.ok(agentTaskMapper.selectList(qw));
    }

    @Operation(summary = "T1: 删除 Agent 历史记录 (V6.8.2 鉴权: 只能删自己的)")
    @DeleteMapping("/history/{id}")
    public Result<Boolean> deleteAgentHistory(@PathVariable Long id,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new SecurityException("需要登录");
        AgentTask task = agentTaskMapper.selectById(id);
        if (task == null) {
            return Result.fail(404, "历史记录不存在");
        }
        if (!user.id().equals(task.getUserId())) {
            return Result.fail(403, "无权删除此历史记录");
        }
        int rows = agentTaskMapper.deleteById(id);
        return Result.ok(rows > 0);
    }

    // ---------- 知识图谱 ----------

    @Operation(summary = "创建/更新实体 (V6.8.2 鉴权)")
    @PostMapping("/kg/entities")
    public Result<Long> upsertEntity(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        Long id = kg.upsertEntity(userId,
                (String) body.get("name"),
                (String) body.get("type"),
                (String) body.get("description"),
                (String) body.get("aliases"),
                body.get("importance") == null ? null : ((Number) body.get("importance")).intValue());
        return Result.ok(id);
    }

    @Operation(summary = "获取实体详情 (V6.8.2 鉴权)")
    @GetMapping("/kg/entities/{id}")
    public Result<KgEntity> getEntity(@PathVariable Long id,
                                       @RequestParam(required = false) Long userId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(kg.getEntity(id, uid));
    }

    @Operation(summary = "搜索实体 (V6.8.2 鉴权)")
    @GetMapping("/kg/entities/search")
    public Result<List<KgEntity>> searchEntities(@RequestParam(required = false) Long userId,
                                                 @RequestParam String keyword,
                                                 @RequestParam(defaultValue = "20") int limit,
                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(kg.searchEntities(uid, keyword, limit));
    }

    @Operation(summary = "删除实体 (V6.8.2 鉴权)")
    @DeleteMapping("/kg/entities/{id}")
    public Result<Boolean> deleteEntity(@PathVariable Long id,
                                        @RequestParam(required = false) Long userId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(kg.deleteEntity(id, uid));
    }

    @Operation(summary = "创建实体关系 (V6.8.2 鉴权)")
    @PostMapping("/kg/relations")
    public Result<Long> createRelation(@RequestBody Map<String, Object> body,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = resolveUserId(user, body);
        Long fromId = body.get("fromId") != null ? ((Number) body.get("fromId")).longValue() : null;
        Long toId = body.get("toId") != null ? ((Number) body.get("toId")).longValue() : null;
        String type = (String) body.get("type");
        String desc = (String) body.get("description");
        Double weight = body.get("weight") == null ? null : ((Number) body.get("weight")).doubleValue();
        return Result.ok(kg.createRelation(userId, fromId, toId, type, desc, weight));
    }

    @Operation(summary = "获取实体1跳邻居 (V6.8.2 鉴权)")
    @GetMapping("/kg/entities/{id}/neighbors")
    public Result<List<Map<String, Object>>> neighbors(@PathVariable Long id,
                                                     @RequestParam(required = false) Long userId,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(kg.neighbors(id, uid));  // V6.8.2: 加 userId 归属校验
    }

    @Operation(summary = "获取实体2跳邻居 (V6.8.2 鉴权)")
    @GetMapping("/kg/entities/{id}/2hop")
    public Result<List<Map<String, Object>>> twoHop(@PathVariable Long id,
                                                    @RequestParam(required = false) Long userId,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(kg.twoHopNeighbors(id, uid));  // V6.8.2: 加 userId 归属校验
    }

    @Operation(summary = "查询最短路径 (V6.8.2 鉴权)")
    @GetMapping("/kg/path")
    public Result<List<KgEntity>> shortestPath(@RequestParam(required = false) Long userId,
                                               @RequestParam Long from,
                                               @RequestParam Long to,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(kg.shortestPath(uid, from, to));
    }

    // ---------- 协作 ----------

    @Operation(summary = "创建协作会话 (V6.8.2 鉴权)")
    @PostMapping("/collab/sessions")
    public Result<Long> createCollab(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        Long ownerId = resolveUserId(user, body);
        Integer max = body.get("maxUsers") == null ? null : ((Number) body.get("maxUsers")).intValue();
        return Result.ok(collab.createSession(ownerId, (String) body.get("title"), max));
    }

    @Operation(summary = "加入协作会话 (V6.8.2 鉴权)")
    @PostMapping("/collab/{id}/join")
    public Result<Boolean> joinCollab(@PathVariable("id") Long collabId,
                                       @RequestParam(required = false) Long userId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(collab.joinSession(collabId, uid, "editor"));
    }

    @Operation(summary = "关闭协作会话 (V6.8.2 鉴权)")
    @PostMapping("/collab/{id}/close")
    public Result<Boolean> closeCollab(@PathVariable("id") Long collabId,
                                        @RequestParam(required = false) Long userId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        Long uid = resolveUserIdParam(user, userId);
        return Result.ok(collab.closeSession(collabId, uid));
    }

    // ---------- 插件市场 ----------

    @Operation(summary = "列出插件 (公开)")
    @GetMapping("/plugins")
    public Result<List<Plugin>> listPlugins(@RequestParam(required = false) String category) {
        return Result.ok(plugin.listAll(category));
    }

    @Operation(summary = "获取插件详情 (公开)")
    @GetMapping("/plugins/{id}")
    public Result<Plugin> getPlugin(@PathVariable Long id) {
        return Result.ok(plugin.get(id));
    }

    @Operation(summary = "发布插件 (V6.8.2 鉴权)")
    @PostMapping("/plugins")
    public Result<Long> publishPlugin(@RequestParam(required = false) Long ownerId,
                                      @RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        Long resolvedOwner = resolveUserIdParam(user, ownerId);
        Long id = plugin.publish(resolvedOwner,
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

    @Operation(summary = "评分插件 (V6.8.2 鉴权)")
    @PostMapping("/plugins/{id}/rate")
    public Result<Boolean> ratePlugin(@PathVariable Long id,
                                      @RequestParam Double score,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new SecurityException("需要登录");
        return Result.ok(plugin.rate(id, score, user.id()));
    }

    @Operation(summary = "启用/禁用插件 (V6.8.2 需管理员)")
    @PostMapping("/plugins/{id}/toggle")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> togglePlugin(@PathVariable Long id,
                                        @RequestParam Boolean enabled) {
        return Result.ok(plugin.setEnabled(id, enabled));
    }

    @Operation(summary = "删除插件 (V6.8.2 鉴权: 只能删除自己的)")
    @DeleteMapping("/plugins/{id}")
    public Result<Boolean> deletePlugin(@PathVariable Long id,
                                        @RequestParam(required = false) Long ownerId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        Long resolvedOwner = resolveUserIdParam(user, ownerId);
        return Result.ok(plugin.delete(id, resolvedOwner));
    }

    @Operation(summary = "调用插件 (公开，但记录调用者)")
    @PostMapping("/plugins/{id}/call")
    public Result<Object> callPlugin(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            // V6.8.2: 记录调用者 userId（用于审计）
            Long userId = user != null ? user.id() : null;
            Object result = plugin.call(id, body != null ? body : Map.of(), userId);
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            return Result.fail(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.fail(403, e.getMessage());
        } catch (Exception e) {
            log.error("[plugin] call failed: {}", e.getMessage());
            return Result.fail(500, "Plugin execution failed: " + e.getMessage());
        }
    }
}
