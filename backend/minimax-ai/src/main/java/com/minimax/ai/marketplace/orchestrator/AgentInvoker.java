package com.minimax.ai.marketplace.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.marketplace.AgentGroupMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 LLM 调用客户端 (T1-backend-orchestrator)
 *
 * <p>通过 HTTP POST 调内部 model 服务 (lb://minimax-model /api/v1/models/chat)
 * 与 {@code minimax-agent} 的 AgentService 模式一致。
 *
 * <p>失败时回退到 deterministic 占位输出, 保证编排流程不被打断 (sandbox 场景)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentInvoker {

    private final ObjectMapper json = new ObjectMapper();

    @Value("${minimax.model.base-url:http://localhost:8092}")
    private String modelBaseUrl;

    @Value("${minimax.model.default-model:deepseek-chat}")
    private String defaultModel;

    @Value("${minimax.gateway.token:}")
    private String token;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 单次对话调用
     *
     * @param member   当前成员 (用于 system prompt 注入)
     * @param goal     用户目标
     * @param context  上文 (PIPELINE 时是上一步输出)
     * @return 完整 LLM 输出
     */
    public String invoke(AgentGroupMember member, String goal, String context) {
        String agentCode = member.getAgentCode();
        String role = member.getRole() == null ? "WORKER" : member.getRole();
        String sysPrompt = buildSystemPrompt(role, member.getConfigJson());
        String userMsg = buildUserMessage(goal, context);

        Map<String, Object> body = new HashMap<>();
        body.put("model", defaultModel);
        body.put("temperature", 0.4);
        body.put("messages", List.of(
                Map.of("role", "system", "content", sysPrompt),
                Map.of("role", "user",   "content", userMsg)
        ));

        try {
            String url = trimSlash(modelBaseUrl) + "/api/v1/models/chat";
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (token != null && !token.isBlank()) {
                hb.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("[agent-invoker] HTTP {} for {} - fallback to echo. body={}",
                        resp.statusCode(), agentCode, truncate(resp.body(), 200));
                return fallback(member, goal, context);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(resp.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) root.get("data");
            if (data == null) return fallback(member, goal, context);
            String content = (String) data.get("content");
            if (content == null || content.isBlank()) return fallback(member, goal, context);
            return content;
        } catch (Exception e) {
            log.warn("[agent-invoker] 调用失败 ({}): {} - fallback", agentCode, e.getMessage());
            return fallback(member, goal, context);
        }
    }

    /**
     * 流式 token 调用 (用 chunked SSE 模式).
     *
     * <p>为了保证编排流程不依赖长连接 model 实现, 这里一次性拿到完整 content,
     * 然后按 5 字符一窗 push 给 emitter (模拟 token 流)。
     *
     * @param onToken  每段 token 触发回调
     * @return 完整输出
     */
    public String invokeStreaming(AgentGroupMember member, String goal, String context, TokenSink onToken) {
        String full = invoke(member, goal, context);
        if (onToken != null && full != null) {
            int chunkSize = 5;
            for (int i = 0; i < full.length(); i += chunkSize) {
                int end = Math.min(full.length(), i + chunkSize);
                try {
                    onToken.accept(full.substring(i, end));
                } catch (Exception ignore) {
                    // 客户端断开, 忽略
                }
            }
        }
        return full == null ? "" : full;
    }

    /** token 回调接口 (避免 Java 8 functional interface 写两遍) */
    @FunctionalInterface
    public interface TokenSink {
        void accept(String token) throws Exception;
    }

    // ---------- helpers ----------

    private String buildSystemPrompt(String role, String configJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 ").append(role).append(" 角色, 在一个多智能体群中负责对应职责。\n");
        switch (role) {
            case "MANAGER" -> sb.append("职责: 拆解目标为子任务, 协调 worker 完成, 汇总最终结果。\n");
            case "CRITIC"  -> sb.append("职责: 评估 worker 输出, 给出 0-10 分 + 改进建议。\n");
            default        -> sb.append("职责: 专注完成自己分配到的子任务, 输出明确, 不超过 200 字。\n");
        }
        if (configJson != null && !configJson.isBlank()) {
            sb.append("成员配置: ").append(configJson).append("\n");
        }
        sb.append("输出要求: 中文, 简洁, 结构化。");
        return sb.toString();
    }

    private String buildUserMessage(String goal, String context) {
        if (context == null || context.isBlank()) {
            return "目标: " + (goal == null ? "" : goal);
        }
        return "目标: " + (goal == null ? "" : goal) + "\n\n上文输出:\n" + context;
    }

    private String fallback(AgentGroupMember member, String goal, String context) {
        // Sandbox / 离线模式占位
        String role = member.getRole() == null ? "WORKER" : member.getRole();
        String agent = member.getAgentCode();
        String ctx = context == null ? "(无)" : truncate(context, 80);
        return String.format("[%s/%s] 已接收目标: %s | 上下文摘要: %s",
                role, agent,
                goal == null ? "(空)" : truncate(goal, 80),
                ctx);
    }

    private static String trimSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
