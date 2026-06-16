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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 通用 OpenAI Chat Completions 兼容适配器。
 * 适用于：OpenAI 官方、Minimax-M3（v1 端点）、Ollama（/v1/chat/completions）、智谱、DeepSeek 等。
 *
 * Day 5 增强：
 *  - chat() 阻塞 + 完整响应
 *  - streamChat() 真实 SSE 解析（BodyHandlers.ofLines）
 *  - 取消机制：通过 stopFlag 控制（同一 provider 实例）
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
        body.put("stream", false);
        try {
            HttpRequest httpReq = buildRequest(endpoint, apiKey, body);
            HttpResponse<String> httpResp = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

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
    public StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                                   Consumer<String> chunkJsonConsumer,
                                   AtomicBoolean stopFlag) {
        return doStreamChat(endpoint, apiKey, req, chunkJsonConsumer, stopFlag);
    }

    public StreamResult doStreamChat(String endpoint, String apiKey, ChatRequest req,
                                     Consumer<String> chunkJsonConsumer,
                                     AtomicBoolean stopFlag) {
        Instant t0 = Instant.now();
        Map<String, Object> body = toOpenAiBody(req);
        body.put("stream", true);
        AtomicInteger ptHolder = new AtomicInteger(0);
        AtomicInteger ctHolder = new AtomicInteger(0);
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder finishHolder = new StringBuilder();
        String id = "stream-" + System.currentTimeMillis();

        try {
            HttpRequest httpReq = buildRequest(endpoint, apiKey, body);
            HttpResponse<java.util.stream.Stream<String>> httpResp =
                    client.send(httpReq, HttpResponse.BodyHandlers.ofLines());

            if (httpResp.statusCode() >= 400) {
                String errBody = httpResp.body().findFirst().orElse("");
                throw new RuntimeException("HTTP " + httpResp.statusCode() + " " + errBody);
            }

            // 逐行解析 SSE
            httpResp.body().forEach(line -> {
                if (stopFlag != null && stopFlag.get()) {
                    throw new RuntimeException("STREAM_CANCELLED");
                }
                if (line == null || line.isEmpty() || !line.startsWith("data:")) return;
                String data = line.substring(5).trim();
                if (data.equals("[DONE]")) return;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> chunk = json.readValue(data, Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                    if (choices == null || choices.isEmpty()) return;
                    Map<String, Object> first = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delta = (Map<String, Object>) first.get("delta");
                    if (delta != null && delta.get("content") != null) {
                        String c = delta.get("content").toString();
                        contentBuilder.append(c);
                        chunkJsonConsumer.accept(data);   // 原样推 OpenAI 的 SSE data
                    }
                    if (first.get("finish_reason") != null) {
                        finishHolder.setLength(0);
                        finishHolder.append(first.get("finish_reason").toString());
                    }
                } catch (Exception e) {
                    log.debug("解析 SSE chunk 失败: {}", e.getMessage());
                }
            });

            return new StreamResult(
                    id, req.getModel(), contentBuilder.toString(),
                    ptHolder.get(), ctHolder.get(), ptHolder.get() + ctHolder.get(),
                    finishHolder.length() == 0 ? "stop" : finishHolder.toString(),
                    Duration.between(t0, Instant.now()).toMillis()
            );
        } catch (Exception e) {
            if ("STREAM_CANCELLED".equals(e.getMessage())) {
                return new StreamResult(
                        id, req.getModel(), contentBuilder.toString(),
                        ptHolder.get(), ctHolder.get(), ptHolder.get() + ctHolder.get(),
                        "cancelled",
                        Duration.between(t0, Instant.now()).toMillis()
                );
            }
            log.error("LLM stream failed: {}", e.getMessage());
            throw new RuntimeException("模型流式调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public reactor.core.publisher.Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        return reactor.core.publisher.Flux.error(new UnsupportedOperationException(
                "OpenAI 流式通过 streamChat() 调用"));
    }

    public record StreamResult(String id, String model, String content,
                               int promptTokens, int completionTokens, int totalTokens,
                               String finishReason, long latencyMs) {}

    // ---------- helpers ----------

    private HttpRequest buildRequest(String endpoint, String apiKey, Map<String, Object> body) throws Exception {
        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(endpoint) + "/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (apiKey != null && !apiKey.isBlank()) {
            hb.header("Authorization", "Bearer " + apiKey);
        }
        return hb.build();
    }

    private Map<String, Object> toOpenAiBody(ChatRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", req.getModel());
        body.put("messages", req.getMessages());
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());
        if (req.getMaxTokens() != null)   body.put("max_tokens", req.getMaxTokens());
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
