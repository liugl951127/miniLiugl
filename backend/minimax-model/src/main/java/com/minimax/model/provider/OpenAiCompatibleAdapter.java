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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 OpenAI Chat Completions 兼容适配器。
 * 适用于：OpenAI 官方、Minimax-M3（v1 端点）、Ollama（/v1/chat/completions）、智谱、DeepSeek 等。
 *
 * 使用 Java 11+ 内置 HttpClient 调上游 (不依赖 webflux)。
 * - chat(): 阻塞调用，解析 choices[0].message.content + usage
 * - stream(): Day 4 阶段仅返回错误提示 (流式待 Day 5 补全)
 */
@Slf4j
@Component
public class OpenAiCompatibleAdapter implements ModelProviderAdapter {

    @Override
    public String code() { return "openai"; }

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public ChatResponse chat(String endpoint, String apiKey, ChatRequest req) {
        Instant t0 = Instant.now();
        Map<String, Object> body = toOpenAiBody(req);
        try {
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(endpoint) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (apiKey != null && !apiKey.isBlank()) {
                hb.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> httpResp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());

            if (httpResp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + httpResp.statusCode() + " " + httpResp.body());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = json.readValue(httpResp.body(), Map.class);
            if (resp == null) throw new RuntimeException("upstream empty response");

            String content = extractContent(resp);
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) resp.getOrDefault("usage", Map.of());
            int pt = toInt(usage.get("prompt_tokens"));
            int ct = toInt(usage.get("completion_tokens"));
            int tt = toInt(usage.get("total_tokens"), pt + ct);

            String id = (String) resp.getOrDefault("id", "chatcmpl-" + System.currentTimeMillis());
            String model = (String) resp.getOrDefault("model", req.getModel());
            String finish = firstFinishReason(resp);

            return ChatResponse.builder()
                    .id(id).model(model).content(content)
                    .promptTokens(pt).completionTokens(ct).totalTokens(tt)
                    .finishReason(finish)
                    .latencyMs(Duration.between(t0, Instant.now()).toMillis())
                    .raw(resp)
                    .build();
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public reactor.core.publisher.Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        return reactor.core.publisher.Flux.error(new UnsupportedOperationException(
                "OpenAI 流式调用在 Day 5 接入，本阶段请用阻塞 /chat 端点"));
    }

    // ---------- helpers ----------

    private Map<String, Object> toOpenAiBody(ChatRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", req.getModel());
        body.put("messages", req.getMessages());
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());
        if (req.getMaxTokens() != null)   body.put("max_tokens", req.getMaxTokens());
        body.put("stream", Boolean.TRUE.equals(req.getStream()));
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> resp) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        Map<String, Object> first = choices.get(0);
        Map<String, Object> msg = (Map<String, Object>) first.get("message");
        if (msg != null) {
            Object c = msg.get("content");
            return c == null ? "" : c.toString();
        }
        Object delta = first.get("delta");
        if (delta instanceof Map) {
            Object c = ((Map<?, ?>) delta).get("content");
            return c == null ? "" : c.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String firstFinishReason(Map<String, Object> resp) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Object r = choices.get(0).get("finish_reason");
        return r == null ? null : r.toString();
    }

    private int toInt(Object v) { return toInt(v, 0); }
    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s);
    }
}
