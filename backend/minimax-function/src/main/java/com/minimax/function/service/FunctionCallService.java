package com.minimax.function.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.function.entity.FunctionCallLog;
import com.minimax.function.entity.FunctionTool;
import com.minimax.function.executor.ToolExecutor;
import com.minimax.function.mapper.FunctionCallLogMapper;
import com.minimax.function.mapper.FunctionToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 * Function Calling 主服务。
 *
 * 流程 (chat with tools):
 *  1) 把启用工具的 JSON Schema 转成 OpenAI tools 格式
 *  2) 拼 messages 调 LLM
 *  3) 若 LLM 返回 tool_calls: 循环执行工具 → 把结果以 role=tool 回传 → 再次 LLM
 *  4) 最多 N 轮 (默认 5)
 *  5) 全部 tool call 写审计
 *
 * 降级:
 *  - LLM 失败: 返回 "[function unavailable] 你的问题: ..."
 *  - 工具失败: 仍把错误结果回传 LLM, 继续循环
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallService {

    @Value("${minimax.function.chat.model:MiniMax-Text-01}")
    private String model;
    @Value("${minimax.function.chat.base-url:http://localhost:8083}")
    private String baseUrl;
    @Value("${minimax.function.chat.token:}")
    private String token;
    @Value("${minimax.function.max-rounds:5}")
    private int maxRounds;
    @Value("${minimax.function.chat.timeout-seconds:30}")
    private int timeout;

    private final FunctionToolService toolService;
    private final FunctionToolMapper toolMapper;
    private final ToolExecutor executor;
    private final FunctionCallLogMapper logMapper;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * 单轮 chat (无 tool)
     */
    public ChatResult chat(Long userId, Long sessionId, String userMessage,
                            List<Map<String, String>> history, boolean enableTools) {
        if (enableTools) {
            return chatWithTools(userId, sessionId, userMessage, history, List.of());
        }
        return plainChat(userId, sessionId, userMessage, history);
    }

    /**
     * 带工具的 chat 循环。
     */
    public ChatResult chatWithTools(Long userId, Long sessionId, String userMessage,
                                     List<Map<String, String>> history, List<String> toolNames) {
        long t0 = System.currentTimeMillis();
        // 1) 准备 tools
        List<FunctionTool> tools = pickTools(toolNames);
        if (tools.isEmpty()) {
            return plainChat(userId, sessionId, userMessage, history);
        }
        List<Map<String, Object>> openAiTools = toOpenAiTools(tools);

        // 2) 准备 messages
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content",
                "你是智能助手。当用户问题需要工具时，调用工具获取准确信息；引用工具结果回答。"));
        if (history != null) messages.addAll(toHistoryMessages(history));
        messages.add(Map.of("role","user","content", userMessage));

        // 3) 循环
        List<ToolCallRecord> toolCalls = new ArrayList<>();
        for (int round = 0; round < maxRounds; round++) {
            LlmResponse resp;
            try {
                resp = callLlm(messages, openAiTools);
            } catch (Exception e) {
                log.warn("LLM 调用失败: {}", e.getMessage());
                return new ChatResult("[function unavailable] 你的问题: " + userMessage,
                        toolCalls, System.currentTimeMillis() - t0);
            }
            // 没 tool_calls → 终止
            if (resp.toolCalls == null || resp.toolCalls.isEmpty()) {
                return new ChatResult(resp.content == null ? "" : resp.content,
                        toolCalls, System.currentTimeMillis() - t0);
            }
            // 有 tool_calls → assistant message + 每个 tool 一个 tool message
            Map<String, Object> asstMsg = new HashMap<>();
            asstMsg.put("role", "assistant");
            if (resp.content != null) asstMsg.put("content", resp.content);
            asstMsg.put("tool_calls", resp.toolCalls);
            messages.add(asstMsg);

            for (LlmToolCall call : resp.toolCalls) {
                long ts = System.currentTimeMillis();
                String argsJson = call.function.arguments == null ? "{}" : call.function.arguments;
                Map<String, Object> argsMap;
                try {
                    argsMap = json.readValue(argsJson, Map.class);
                } catch (Exception e) {
                    argsMap = Map.of("__raw__", argsJson);
                }
                String result;
                String status = "ok";
                String errMsg = null;
                try {
                    result = executor.executeForChat(call.function.name, argsMap);
                } catch (Exception e) {
                    log.warn("tool exec failed: name={} err={}", call.function.name, e.getMessage());
                    result = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
                    status = "error";
                    errMsg = e.getMessage();
                }
                int dur = (int) (System.currentTimeMillis() - ts);
                // 审计
                FunctionCallLog log = new FunctionCallLog();
                log.setUserId(userId);
                log.setSessionId(sessionId);
                log.setToolName(call.function.name);
                log.setArguments(argsJson);
                log.setResult(truncate(result, 2000));
                log.setStatus(status);
                log.setErrorMsg(truncate(errMsg, 500));
                log.setDurationMs(dur);
                try { logMapper.insert(log); } catch (Exception ignore) {}
                // 记录返回
                toolCalls.add(new ToolCallRecord(call.id, call.function.name, argsJson, result, status, dur));
                // 加 tool message
                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", call.id);
                toolMsg.put("name", call.function.name);
                toolMsg.put("content", result);
                messages.add(toolMsg);
            }
        }
        // 超 maxRounds
        return new ChatResult("[function loop exceeded " + maxRounds + " rounds]", toolCalls,
                System.currentTimeMillis() - t0);
    }

    private ChatResult plainChat(Long userId, Long sessionId, String userMessage,
                                  List<Map<String, String>> history) {
        long t0 = System.currentTimeMillis();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content","你是助手。"));
        if (history != null) messages.addAll(toHistoryMessages(history));
        messages.add(Map.of("role","user","content", userMessage));
        try {
            LlmResponse resp = callLlm(messages, null);
            return new ChatResult(resp.content == null ? "" : resp.content,
                    List.of(), System.currentTimeMillis() - t0);
        } catch (Exception e) {
            return new ChatResult("[unavailable] " + e.getMessage(),
                    List.of(), System.currentTimeMillis() - t0);
        }
    }

    private List<FunctionTool> pickTools(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return toolService.listAll();
        }
        List<FunctionTool> out = new ArrayList<>();
        for (String n : toolNames) {
            FunctionTool t = toolMapper.selectByName(n);
            if (t != null && t.getEnabled() != null && t.getEnabled() == 1) out.add(t);
        }
        return out;
    }

    private List<Map<String, Object>> toOpenAiTools(List<FunctionTool> tools) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FunctionTool t : tools) {
            Map<String, Object> fn = new HashMap<>();
            fn.put("name", t.getName());
            fn.put("description", t.getDescription());
            // parameters: 解析为 map, 失败则作为空 object
            try {
                Map<String, Object> params = json.readValue(t.getParameters(), Map.class);
                fn.put("parameters", params);
            } catch (Exception e) {
                fn.put("parameters", Map.of("type", "object", "properties", Map.of()));
            }
            out.add(Map.of("type", "function", "function", fn));
        }
        return out;
    }

    private List<Map<String, Object>> toHistoryMessages(List<Map<String, String>> history) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, String> h : history) {
            String role = h.get("role");
            String content = h.get("content");
            if (role == null || content == null) continue;
            out.add(Map.of("role", role, "content", content));
        }
        return out;
    }

    private LlmResponse callLlm(List<Map<String, Object>> messages, List<Map<String, Object>> tools)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        if (tools != null && !tools.isEmpty()) body.put("tools", tools);

        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(baseUrl) + "/api/v1/models/chat"))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (token != null && !token.isBlank()) hb.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> dataInner = (Map<String, Object>) data.get("data");
        if (dataInner == null) {
            return new LlmResponse("", null);
        }
        String content = (String) dataInner.get("content");
        // 注意: 真实 LLM 应在 message 里返回 tool_calls. 这里 mock 实现可能没有.
        // 为兼容: 解析 message.tool_calls
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) dataInner.get("message");
        List<LlmToolCall> toolCalls = null;
        if (message != null && message.get("tool_calls") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tcs = (List<Map<String, Object>>) message.get("tool_calls");
            toolCalls = new ArrayList<>();
            for (Map<String, Object> tc : tcs) {
                LlmToolCall call = new LlmToolCall();
                call.id = (String) tc.get("id");
                @SuppressWarnings("unchecked")
                Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                if (fn != null) {
                    call.function = new LlmToolCall.Fn();
                    call.function.name = (String) fn.get("name");
                    call.function.arguments = (String) fn.get("arguments");
                }
                toolCalls.add(call);
            }
        }
        return new LlmResponse(content == null ? "" : content, toolCalls);
    }

    private String stripSlash(String s) { return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s); }
    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }

    // ---- DTOs ----

    public static class LlmResponse {
        public String content;
        public List<LlmToolCall> toolCalls;
        public LlmResponse(String c, List<LlmToolCall> t) { this.content = c; this.toolCalls = t; }
    }

    public static class LlmToolCall {
        public String id;
        public Fn function;
        public static class Fn { public String name; public String arguments; }
    }

    public record ToolCallRecord(String id, String name, String arguments,
                                  String result, String status, int durationMs) {}

    public record ChatResult(String answer, List<ToolCallRecord> toolCalls, long durationMs) {}
}
