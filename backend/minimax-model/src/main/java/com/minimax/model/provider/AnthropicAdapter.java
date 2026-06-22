package com.minimax.model.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * V5.18: Anthropic Claude 适配器 (Anthropic Messages API).
 *
 * 协议差异 vs OpenAI:
 *   - 端点: POST {endpoint}/v1/messages
 *   - Header: x-api-key: {key}, anthropic-version: 2023-06-01
 *   - Body: { model, messages:[{role, content}], max_tokens, system, stream }
 *   - 响应: { content:[{type:"text", text:"..."}], stop_reason, usage:{input_tokens,output_tokens} }
 *
 * 适用: Claude 3.5 Sonnet / Claude 3 Opus / Claude 3 Haiku
 *
 * 流式 SSE 格式: event: message_start / content_block_start / content_block_delta / message_delta / message_stop
 * 解析: 提取 content_block_delta.delta.text 拼成最终内容
 */
@Slf4j
@Component
public class AnthropicAdapter implements ModelProviderAdapter {

    @Override
    public String code() { return "anthropic"; }

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public ChatResponse chat(String endpoint, String apiKey, ChatRequest req) {
        Instant t0 = Instant.now();
        Map<String, Object> body = toAnthropicBody(req, false);
        try {
            HttpRequest httpReq = buildRequest(endpoint, apiKey, body);
            HttpResponse<String> resp = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(resp.body(), Map.class);
            return parseAnthropicResponse(root, req, t0);
        } catch (Exception e) {
            log.error("Anthropic call failed: {}", e.getMessage());
            throw new RuntimeException("Anthropic 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public OpenAiCompatibleAdapter.StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                                                          Consumer<String> chunkJsonConsumer, AtomicBoolean stopFlag) {
        Instant t0 = Instant.now();
        Map<String, Object> body = toAnthropicBody(req, true);
        StringBuilder contentBuilder = new StringBuilder();
        AtomicInteger inputTokens = new AtomicInteger(0);
        AtomicInteger outputTokens = new AtomicInteger(0);
        String finish = "stop";

        try {
            HttpRequest httpReq = buildRequest(endpoint, apiKey, body);
            HttpResponse<java.util.stream.Stream<String>> resp = client.send(httpReq,
                    HttpResponse.BodyHandlers.ofLines());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            resp.body().forEach(line -> {
                if (stopFlag != null && stopFlag.get()) {
                    throw new RuntimeException("STREAM_CANCELLED");
                }
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) return;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ev = json.readValue(data, Map.class);
                        String type = (String) ev.get("type");
                        if ("content_block_delta".equals(type)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> delta = (Map<String, Object>) ev.get("delta");
                            if (delta != null) {
                                Object text = delta.get("text");
                                if (text != null) {
                                    String chunk = text.toString();
                                    contentBuilder.append(chunk);
                                    if (chunkJsonConsumer != null) chunkJsonConsumer.accept(chunk);
                                }
                            }
                        } else if ("message_delta".equals(type)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> delta = (Map<String, Object>) ev.get("delta");
                            if (delta != null && delta.get("stop_reason") != null) {
                                finish = delta.get("stop_reason").toString();
                            }
                        } else if ("message_start".equals(type)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> msg = (Map<String, Object>) ev.get("message");
                            if (msg != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> usage = (Map<String, Object>) msg.get("usage");
                                if (usage != null) {
                                    inputTokens.set(toInt(usage.get("input_tokens")));
                                }
                            }
                        } else if ("message_stop".equals(type)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> msg = (Map<String, Object>) ev.get("message");
                            if (msg != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> usage = (Map<String, Object>) msg.get("usage");
                                if (usage != null) {
                                    outputTokens.set(toInt(usage.get("output_tokens")));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Anthropic SSE parse err: {}", e.getMessage());
                    }
                }
            });

            return new OpenAiCompatibleAdapter.StreamResult(
                null,                       // id
                req.getModel(),             // model
                contentBuilder.toString(),  // content
                inputTokens.get(),          // promptTokens
                outputTokens.get(),         // completionTokens
                inputTokens.get() + outputTokens.get(),  // totalTokens
                finish,                     // finishReason
                Duration.between(t0, Instant.now()).toMillis()  // latencyMs
            );
        } catch (Exception e) {
            log.error("Anthropic stream failed: {}", e.getMessage());
            throw new RuntimeException("Anthropic 流式失败: " + e.getMessage(), e);
        }
    }

    @Override
    public reactor.core.publisher.Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        // 简化: 不实现 webflux Flux, 走 streamChat
        throw new UnsupportedOperationException("Use streamChat instead");
    }

    // ---- 协议转换 ----

    /**
     * OpenAI ChatRequest → Anthropic Messages body
     */
    private Map<String, Object> toAnthropicBody(ChatRequest req, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", req.getModel());
        body.put("max_tokens", req.getMaxTokens() != null ? req.getMaxTokens() : 4096);
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());

        // messages (V5.30.4: ChatRequest.messages 是 List<Map<String, String>>, 用 Map 迭代)
        List<Map<String, Object>> messages = new ArrayList<>();
        String systemPrompt = null;
        for (Map<String, String> m : req.getMessages()) {
            String role = m.get("role");
            String content = m.get("content");
            if ("system".equals(role)) {
                if (systemPrompt == null) systemPrompt = content;
                else systemPrompt += "\n" + content;
            } else {
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", role);
                msg.put("content", content);
                messages.add(msg);
            }
        }
        if (systemPrompt != null) body.put("system", systemPrompt);
        body.put("messages", messages);
        if (stream) body.put("stream", true);
        return body;
    }

    private HttpRequest buildRequest(String endpoint, String apiKey, Map<String, Object> body) throws Exception {
        String url = stripSlash(endpoint) + "/v1/messages";
        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey != null ? apiKey : "")
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return hb.build();
    }

    private ChatResponse parseAnthropicResponse(Map<String, Object> root, ChatRequest req, Instant t0) {
        String content = "";
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) root.get("content");
            if (contentList != null) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> c : contentList) {
                    if ("text".equals(c.get("type")) && c.get("text") != null) {
                        sb.append(c.get("text"));
                    }
                }
                content = sb.toString();
            }
        } catch (Exception e) {
            log.warn("Anthropic parse content failed: {}", e.getMessage());
        }
        int pt = 0, ct = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) root.get("usage");
            if (usage != null) {
                pt = toInt(usage.get("input_tokens"));
                ct = toInt(usage.get("output_tokens"));
            }
        } catch (Exception ignore) {}
        String finish = (String) root.getOrDefault("stop_reason", "stop");

        return ChatResponse.builder()
                .id((String) root.getOrDefault("id", "anthropic-" + System.currentTimeMillis()))
                .model((String) root.getOrDefault("model", req.getModel()))
                .content(content)
                .promptTokens(pt).completionTokens(ct).totalTokens(pt + ct)
                .finishReason(finish)
                .latencyMs(Duration.between(t0, Instant.now()).toMillis())
                .raw(root)
                .build();
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length() - 1) : s);
    }

    private String truncate(String s, int n) {
        return s == null ? null : (s.length() > n ? s.substring(0, n) : s);
    }
}
