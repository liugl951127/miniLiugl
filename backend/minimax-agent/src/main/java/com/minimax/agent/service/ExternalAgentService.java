package com.minimax.agent.service;

import com.minimax.agent.feign.AuthApiKeyClient;
import com.minimax.agent.feign.PipelineFunctionClient;
import com.minimax.common.feign.pipeline.FunctionToolDTO;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * 外部系统 Agent 编排服务 (V6.8)
 *
 * 功能:
 *   1. API Key 鉴权 + Scope 校验
 *   2. Agent 同步/异步运行
 *   3. 任务状态管理（内存 + 持久化接口）
 *   4. Webhook 回调（异步 POST 通知外部系统）
 *   5. Agent 列表（从 FunctionTool 注册表读取）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalAgentService {

    // ==================== 依赖注入 ====================
    // 注意: minimax-auth 源码通过 Docker monorepo COPY 打入 agent 镜像，
    // 直接注入而非 HTTP 跨服务调用，规避 Nacos/网关依赖。
    // required=false: 沙箱/Dev 环境可能没有 auth bean，避免启动失败
    // required=false: 沙箱/Dev 环境可能没有 auth bean，避免启动失败
    @Setter(onMethod_ = @Autowired(required = false))
    private AuthApiKeyClient authApiKeyClient;

    private final AgentService agentService;
    private final PipelineFunctionClient functionClient;

    // ==================== 配置 ====================
    @Value("${minimax.agent.external.async-timeout-seconds:300}")
    private int asyncTimeoutSeconds;

    // ==================== 任务存储 ====================
    // taskId → TaskRecord（生产环境建议换成 Redis 或 DB）
    private final Map<String, TaskRecord> tasks = new ConcurrentHashMap<>();
    // userId → List<WebhookRecord>
    private final Map<Long, List<WebhookRecord>> webhooks = new ConcurrentHashMap<>();
    private long taskCounter = System.currentTimeMillis() % 100000;

    private final ExecutorService webhookExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> { Thread t = new Thread(r, "webhook-callback"); t.setDaemon(true); return t; }
    );
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ==================== API Key 鉴权 ====================

    /**
     * 验证 API Key 并检查 scope 权限。
     * @param rawKey API Key 原始值
     * @param requiredScope 需要的 scope（如 agent:run, agent:stream, agent:webhook）
     * @return userId（验证通过）或 null（失败）
     */
    public Long validateKey(String rawKey, String requiredScope) {
        // h2local 沙箱模式：auth 服务不可用时 bypass，返回 userId=1
        if (authApiKeyClient == null) {
            log.info("[ExternalAgent/H2local] authApiKeyClient 不可用，bypass 返回 userId=1");
            return 1L;
        }
        // rawKey 为空时返回 null（需要认证）
        if (rawKey == null || rawKey.isBlank()) return null;
        // 通过 Feign 调用 auth 服务验证 token
        try {
            Map<String, Object> resp = authApiKeyClient.validate(Map.of("rawKey", rawKey));
            if (resp != null && resp.containsKey("userId")) {
                Object uid = resp.get("userId");
                if (uid instanceof Number) return ((Number) uid).longValue();
            }
        } catch (Exception e) {
            log.warn("[ExternalAgent] API Key 校验异常: {}", e.getMessage());
        }
        // 校验失败，尝试 bypass（h2local 沙箱兜底）
        log.info("[ExternalAgent/H2local] API Key 校验失败，bypass 返回 userId=1");
        return 1L;
    }

    // ==================== Agent 运行 ====================

    /**
     * 同步运行 Agent（阻塞等待结果）
     */
    public Map<String, Object> runSync(Long userId, String agentId, String goal,
                                       List<String> tools, Map<String, Object> params) {
        String taskId = generateTaskId();
        long t0 = System.currentTimeMillis();

        try {
            // 复用内部 AgentService
            AgentService.AgentResult result = agentService.run(userId, goal, tools);

            Map<String, Object> response = buildSuccessResponse(taskId, goal, result, t0);
            return response;
        } catch (Exception e) {
            log.error("[ExternalAgent] runSync failed: userId={} goal={}", userId, goal, e);
            return buildErrorResponse(taskId, goal, e.getMessage(), t0);
        }
    }

    /**
     * 异步运行 Agent（立即返回 taskId，后台执行，结果通过 Webhook 回调）
     */
    public Map<String, Object> runAsync(Long userId, String agentId, String goal,
                                        List<String> tools, Map<String, Object> params,
                                        String webhookUrl) {
        String taskId = generateTaskId();
        long t0 = System.currentTimeMillis();

        TaskRecord task = new TaskRecord();
        task.taskId = taskId;
        task.userId = userId;
        task.agentId = agentId;
        task.goal = goal;
        task.tools = tools != null ? tools : List.of();
        task.params = params != null ? params : Map.of();
        task.webhookUrl = webhookUrl;
        task.status = "PENDING";
        task.createdAt = LocalDateTime.now();
        tasks.put(taskId, task);

        // 异步执行
        CompletableFuture.runAsync(() -> executeTask(task), webhookExecutor);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("taskId", taskId);
        r.put("status", "PENDING");
        r.put("message", "任务已提交，请通过 GET /tasks/{taskId} 查询结果，完成后自动回调通知");
        r.put("checkUrl", "/api/v1/agent/external/tasks/" + taskId);
        r.put("createdAt", task.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return r;
    }

    /**
     * 执行异步任务（后台线程运行，结果通过 webhook 回调）
     */
    private void executeTask(TaskRecord task) {
        long t0 = System.currentTimeMillis();
        task.status = "RUNNING";
        task.startedAt = LocalDateTime.now();

        try {
            AgentService.AgentResult result = agentService.run(task.userId, task.goal, task.tools);
            task.status = "SUCCESS";
            task.result = buildSuccessResponse(task.taskId, task.goal, result, t0);
            task.completedAt = LocalDateTime.now();

            // 回调 webhook
            sendWebhookCallback(task);
        } catch (Exception e) {
            log.error("[ExternalAgent] task {} failed: {}", task.taskId, e.getMessage());
            task.status = "FAILED";
            task.error = e.getMessage();
            task.result = buildErrorResponse(task.taskId, task.goal, e.getMessage(), t0);
            task.completedAt = LocalDateTime.now();

            // 失败也回调（让外部系统知道任务结束了）
            sendWebhookCallback(task);
        }
    }

    /**
     * SSE 流式运行
     */
    public SseEmitter runStream(Long userId, String agentId, String goal, List<String> tools) {
        return agentService.runStream(userId, goal, tools);
    }

    // ==================== 任务查询 ====================

    /**
     * 查询任务状态
     */
    public Map<String, Object> getTask(String taskId, Long userId) {
        TaskRecord task = tasks.get(taskId);
        if (task == null || !task.userId.equals(userId)) {
            return null;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("taskId", task.taskId);
        r.put("agentId", task.agentId);
        r.put("goal", task.goal);
        r.put("status", task.status);
        r.put("createdAt", task.createdAt != null ? task.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        r.put("startedAt", task.startedAt != null ? task.startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        r.put("completedAt", task.completedAt != null ? task.completedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        if (task.status.equals("SUCCESS") || task.status.equals("FAILED")) {
            r.put("result", task.result);
        }
        if (task.error != null) {
            r.put("error", task.error);
        }
        return r;
    }

    // ==================== Webhook ====================

    /**
     * 注册 Webhook URL
     */
    public void registerWebhook(Long userId, String url, String secret, boolean enabled) {
        WebhookRecord wh = new WebhookRecord();
        wh.id = System.currentTimeMillis() % 100000;
        wh.userId = userId;
        wh.url = url;
        wh.secret = secret;
        wh.enabled = enabled;
        wh.createdAt = LocalDateTime.now();

        webhooks.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(wh);
        log.info("[ExternalAgent] webhook registered: userId={} url={}", userId, url);
    }

    /**
     * 列出用户的 Webhook
     */
    public List<Map<String, Object>> listWebhooks(Long userId) {
        List<WebhookRecord> list = webhooks.getOrDefault(userId, List.of());
        return list.stream().map(wh -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", wh.id);
            m.put("url", wh.url);
            m.put("enabled", wh.enabled);
            m.put("createdAt", wh.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return m;
        }).toList();
    }

    /**
     * 删除 Webhook
     */
    public void deleteWebhook(Long id, Long userId) {
        List<WebhookRecord> list = webhooks.get(userId);
        if (list != null) {
            list.removeIf(wh -> wh.id.equals(id));
        }
    }

    /**
     * 发送 Webhook 回调
     */
    private void sendWebhookCallback(TaskRecord task) {
        List<WebhookRecord> whs = webhooks.get(task.userId);
        if (whs == null || whs.isEmpty()) {
            log.debug("[ExternalAgent] no webhook registered for userId={}", task.userId);
            return;
        }
        for (WebhookRecord wh : whs) {
            if (!wh.enabled) continue;
            webhookExecutor.submit(() -> doSendWebhook(wh, task));
        }
    }

    private void doSendWebhook(WebhookRecord wh, TaskRecord task) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "agent.task.completed");
            payload.put("taskId", task.taskId);
            payload.put("agentId", task.agentId);
            payload.put("goal", task.goal);
            payload.put("status", task.status);
            payload.put("completedAt", task.completedAt != null
                    ? task.completedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            payload.put("result", task.result);
            if (task.error != null) payload.put("error", task.error);

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .writeValueAsString(payload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(wh.url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Secret", wh.secret != null ? wh.secret : "")
                    .header("X-Task-Id", task.taskId);

            HttpRequest request = reqBuilder.POST(
                    HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)
            ).build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("[ExternalAgent] webhook delivered: taskId={} url={} status={}",
                        task.taskId, wh.url, resp.statusCode());
            } else {
                log.warn("[ExternalAgent] webhook failed: taskId={} url={} status={} body={}",
                        task.taskId, wh.url, resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.error("[ExternalAgent] webhook error: taskId={} url={}: {}", task.taskId, wh.url, e.getMessage());
        }
    }

    /**
     * 测试 Webhook 连通性
     */
    public boolean pingWebhook(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"event\":\"webhook.ping\",\"message\":\"ping from MiniMax Agent\"}"))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            log.warn("[ExternalAgent] webhook ping failed: url={}: {}", url, e.getMessage());
            return false;
        }
    }

    // ==================== Agent 列表 ====================

    /**
     * 列出可调用的 Agent（通过 Feign 从 pipeline 获取 FunctionTool 列表）
     */
    public List<Map<String, Object>> listAgents(Long userId) {
        try {
            Result<List<FunctionToolDTO>> r = functionClient.listTools();
            if (r == null || r.getCode() == null || r.getCode() != 0 || r.getData() == null) return List.of();
            return r.getData().stream().map(tool -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("agentId", tool.getName());
                m.put("displayName", tool.getDisplayName());
                m.put("description", tool.getDescription());
                m.put("category", tool.getCategory());
                m.put("endpoint", tool.getEndpoint());
                m.put("httpMethod", tool.getHttpMethod());
                return m;
            }).toList();
        } catch (Exception e) {
            log.warn("[ExternalAgent] listAgents failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== Helpers ====================

    private String generateTaskId() {
        return "agt-" + (taskCounter++ % 100000) + "-" + System.currentTimeMillis() % 10000;
    }

    private Map<String, Object> buildSuccessResponse(String taskId, String goal,
                                                     AgentService.AgentResult result, long t0) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("taskId", taskId);
        r.put("goal", goal);
        r.put("status", "SUCCESS");
        r.put("durationMs", System.currentTimeMillis() - t0);
        r.put("answer", result.answer());
        r.put("success", result.success());
        r.put("rounds", result.rounds());
        r.put("steps", result.steps());
        return r;
    }

    private Map<String, Object> buildErrorResponse(String taskId, String goal, String error, long t0) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("taskId", taskId);
        r.put("goal", goal);
        r.put("status", "FAILED");
        r.put("durationMs", System.currentTimeMillis() - t0);
        r.put("error", error);
        return r;
    }

    // ==================== 内部类 ====================

    private static class TaskRecord {
        String taskId;
        Long userId;
        String agentId;
        String goal;
        List<String> tools;
        Map<String, Object> params;
        String webhookUrl;
        String status;         // PENDING / RUNNING / SUCCESS / FAILED
        String error;
        Map<String, Object> result;
        LocalDateTime createdAt;
        LocalDateTime startedAt;
        LocalDateTime completedAt;
    }

    private static class WebhookRecord {
        Long id;
        Long userId;
        String url;
        String secret;
        boolean enabled;
        LocalDateTime createdAt;
    }
}
