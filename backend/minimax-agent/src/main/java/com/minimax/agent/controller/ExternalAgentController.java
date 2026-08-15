package com.minimax.agent.controller;

import com.minimax.agent.service.ExternalAgentService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 外部系统 Agent 调用 API (V6.8)
 *
 * 外部系统通过 API Key 鉴权，调用编排的智能体。
 *
 * 鉴权方式:
 *   Header: Authorization: Bearer <api_key>
 *   或    Header: X-API-Key: <api_key>
 *
 * Webhook 回调:
 *   异步任务完成后 POST 回调通知外部系统（需注册 webhook URL）
 *
 * 接口:
 *   POST /api/v1/agent/external/run           同步运行 Agent
 *   POST /api/v1/agent/external/run-async    异步运行（返回 taskId）
 *   GET  /api/v1/agent/external/tasks/{id}   查询任务状态
 *   POST /api/v1/agent/external/webhook      注册 Webhook URL
 *   GET  /api/v1/agent/external/agents       列出可调用的 Agent
 *   POST /api/v1/agent/external/run-stream   SSE 流式运行
 */
@Slf4j
@Tag(name = "外部系统 Agent API")
@RestController
@RequestMapping("/api/v1/agent/external")
@RequiredArgsConstructor
public class ExternalAgentController {

    private final ExternalAgentService externalAgentService;

    // ==================== Agent 运行 ====================

    /**
     * 同步运行 Agent（等待结果，最多 60s）
     * 用于快速验证或简单任务
     */
    @Operation(summary = "同步运行 Agent（同步返回结果，最多 60s）")
    @PostMapping("/run")
    public Result<Map<String, Object>> run(@RequestBody Map<String, Object> req,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:run");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");

        String agentId = (String) req.get("agentId");
        String goal = (String) req.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) req.get("tools");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) req.get("params");

        log.info("[ExternalAgent] userId={} agentId={} goal={}", userId, agentId, goal);
        Map<String, Object> result = externalAgentService.runSync(userId, agentId, goal, tools, params);
        return Result.ok(result);
    }

    /**
     * 异步运行 Agent（立即返回 taskId，结果通过 Webhook 回调）
     * 用于复杂任务或长时间运行
     */
    @Operation(summary = "异步运行 Agent（立即返回 taskId，结果通过 Webhook 回调）")
    @PostMapping("/run-async")
    public Result<Map<String, Object>> runAsync(@RequestBody Map<String, Object> req,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:run");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");

        String agentId = (String) req.get("agentId");
        String goal = (String) req.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) req.get("tools");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) req.get("params");
        String webhookUrl = (String) req.get("webhookUrl");

        log.info("[ExternalAgent] async userId={} agentId={} webhook={}", userId, agentId, webhookUrl);
        Map<String, Object> task = externalAgentService.runAsync(userId, agentId, goal, tools, params, webhookUrl);
        return Result.ok(task);
    }

    /**
     * 查询异步任务状态
     */
    @Operation(summary = "查询异步任务状态")
    @GetMapping("/tasks/{taskId}")
    public Result<Map<String, Object>> getTask(@PathVariable String taskId,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:run");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");

        Map<String, Object> task = externalAgentService.getTask(taskId, userId);
        if (task == null) return Result.fail(404, "任务不存在");
        return Result.ok(task);
    }

    /**
     * SSE 流式运行（实时推送 Agent 思考过程）
     */
    @Operation(summary = "SSE 流式运行 Agent（实时推送思考过程）")
    @PostMapping(value = "/run-stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@RequestBody Map<String, Object> req,
                                @RequestHeader(value = "Authorization", required = false) String authHeader,
                                @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:stream");
        if (userId == null) {
            // 返回一个错误 SSE 事件然后关闭
            SseEmitter emitter = new SseEmitter(0L);
            try { emitter.send(SseEmitter.event().name("error").data("{\"code\":401,\"msg\":\"无效 API Key\"}")); } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        String agentId = (String) req.get("agentId");
        String goal = (String) req.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) req.get("tools");
        return externalAgentService.runStream(userId, agentId, goal, tools);
    }

    // ==================== Webhook 管理 ====================

    /**
     * 注册 Webhook URL（异步任务完成时回调通知）
     */
    @Operation(summary = "注册 Webhook URL（异步任务完成时回调）")
    @PostMapping("/webhook")
    public Result<Void> registerWebhook(@RequestBody Map<String, Object> req,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:webhook");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");

        String url = (String) req.get("url");
        String secret = (String) req.get("secret");
        Boolean enabled = req.get("enabled") == null ? true : (Boolean) req.get("enabled");
        externalAgentService.registerWebhook(userId, url, secret, enabled);
        return Result.ok();
    }

    /**
     * 列出已注册的 Webhook
     */
    @Operation(summary = "列出 Webhook")
    @GetMapping("/webhooks")
    public Result<List<Map<String, Object>>> listWebhooks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:webhook");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");
        return Result.ok(externalAgentService.listWebhooks(userId));
    }

    /**
     * 删除 Webhook
     */
    @Operation(summary = "删除 Webhook")
    @DeleteMapping("/webhook/{id}")
    public Result<Void> deleteWebhook(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String authHeader,
                                     @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:webhook");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");
        externalAgentService.deleteWebhook(id, userId);
        return Result.ok();
    }

    // ==================== Agent 列表 ====================

    /**
     * 列出当前用户可调用的 Agent
     */
    @Operation(summary = "列出可调用的 Agent")
    @GetMapping("/agents")
    public Result<List<Map<String, Object>>> listAgents(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        log.info("[Controller] listAgents called, apiKey='{}', authHeader='{}'", apiKey, authHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:run");
        log.info("[Controller] validateKey returned userId={}", userId);
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");
        return Result.ok(externalAgentService.listAgents(userId));
    }

    /**
     * 触发 Webhook 回调（外部系统可验证 webhook 是否可达）
     * GET /api/v1/agent/external/webhook/ping?url=https://...
     */
    @Operation(summary = "测试 Webhook 连通性（发送 ping 请求）")
    @GetMapping("/webhook/ping")
    public Result<String> pingWebhook(@RequestParam String url,
                                     @RequestHeader(value = "Authorization", required = false) String authHeader,
                                     @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String apiKey = resolveApiKey(authHeader, apiKeyHeader);
        Long userId = externalAgentService.validateKey(apiKey, "agent:webhook");
        if (userId == null) return Result.fail(401, "无效或禁用的 API Key");
        boolean ok = externalAgentService.pingWebhook(url);
        return ok ? Result.ok("✅ Webhook 可达") : Result.fail(502, "❌ Webhook 不可达");
    }

    // ---- helpers ----

    private String resolveApiKey(String authHeader, String apiKeyHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return apiKeyHeader;
    }
}
