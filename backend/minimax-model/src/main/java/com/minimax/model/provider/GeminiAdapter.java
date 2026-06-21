package com.minimax.model.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.dto.Message;
import com.minimax.model.vo.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
 * V5.18: Google Gemini 适配器 (Google Generative Language API).
 *
 * 协议:
 *   - 端点: POST {endpoint}/v1beta/models/{model}:generateContent?key={API_KEY}
 *   - Body: { contents:[{role:"user"/"model", parts:[{text:"..."}]}], generationConfig:{...} }
 *   - 响应: { candidates:[{content:{parts:[{text:"..."}]}}], usageMetadata:{...} }
 *
 * 适用: gemini-1.5-pro / gemini-1.5-flash / gemini-2.0-flash
 */
@Slf4j
@Component
public class GeminiAdapter implements ModelProviderAdapter {

    @Override
    public String code() { return "gemini"; }

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public ChatResponse chat(String endpoint, String apiKey, ChatRequest req) {
        Instant t0 = Instant.now();
        try {
            String url = buildUrl(endpoint, apiKey, req.getModel(), false);
            Map<String, Object> body = toGeminiBody(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(resp.body(), Map.class);
            return parseGeminiResponse(root, req, t0);
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage());
            throw new RuntimeException("Gemini 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public OpenAiCompatibleAdapter.StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                                                          Consumer<String> chunkJsonConsumer, AtomicBoolean stopFlag) {
        Instant t0 = Instant.now();
        StringBuilder contentBuilder = new StringBuilder();
        AtomicInteger pt = new AtomicInteger(0), ct = new AtomicInteger(0);
        try {
            String url = buildUrl(endpoint, apiKey, req.getModel(), true);
            Map<String, Object> body = toGeminiBody(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<java.util.stream.Stream<String>> resp = client.send(httpReq,
                    HttpResponse.BodyHandlers.ofLines());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            resp.body().forEach(line -> {
                if (stopFlag != null && stopFlag.get()) throw new RuntimeException("STREAM_CANCELLED");
                if (line.isEmpty()) return;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ev = json.readValue(line, Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) ev.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        if (content != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (parts != null) {
                                for (Map<String, Object> p : parts) {
                                    if (p.get("text") != null) {
                                        String chunk = p.get("text").toString();
                                        contentBuilder.append(chunk);
                                        if (chunkJsonConsumer != null) chunkJsonConsumer.accept(chunk);
                                    }
                                }
                            }
                        }
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> usage = (Map<String, Object>) ev.get("usageMetadata");
                    if (usage != null) {
                        pt.set(toInt(usage.get("promptTokenCount")));
                        ct.set(toInt(usage.get("candidatesTokenCount")));
                    }
                } catch (Exception e) {
                    log.debug("Gemini SSE parse err: {}", e.getMessage());
                }
            });
            return new OpenAiCompatibleAdapter.StreamResult(
                contentBuilder.toString(), pt.get(), ct.get(), "stop",
                Duration.between(t0, Instant.now()).toMillis()
            );
        } catch (Exception e) {
            log.error("Gemini stream failed: {}", e.getMessage());
            throw new RuntimeException("Gemini 流式失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        throw new UnsupportedOperationException("Use streamChat instead");
    }

    // ---- 协议转换 ----

    /**
     * OpenAI ChatRequest → Gemini generateContent body
     */
    private Map<String, Object> toGeminiBody(ChatRequest req) {
        Map<String, Object> body = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        // Gemini 不支持 system role, 合并到第一条 user
        String systemPrefix = "";
        for (Message m : req.getMessages()) {
            if ("system".equals(m.getRole())) {
                systemPrefix += m.getContent() + "\n";
                continue;
            }
            String role = "assistant".equals(m.getRole()) ? "model" : "user";
            Map<String, Object> part = new HashMap<>();
            part.put("text", m.getContent());
            Map<String, Object> content = new HashMap<>();
            content.put("role", role);
            content.put("parts", List.of(part));
            contents.add(content);
        }
        // 如果有 system, 拼到第一条 user
        if (!systemPrefix.isEmpty() && !contents.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> first = (Map<String, Object>) contents.get(0);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) first.get("parts");
            if (parts != null && !parts.isEmpty()) {
                parts.get(0).put("text", systemPrefix + parts.get(0).get("text"));
            }
        }
        body.put("contents", contents);
        // generationConfig
        Map<String, Object> genConfig = new HashMap<>();
        if (req.getMaxTokens() != null) genConfig.put("maxOutputTokens", req.getMaxTokens());
        if (req.getTemperature() != null) genConfig.put("temperature", req.getTemperature());
        if (!genConfig.isEmpty()) body.put("generationConfig", genConfig);
        return body;
    }

    private String buildUrl(String endpoint, String apiKey, String model, boolean stream) {
        String base = stripSlash(endpoint);
        String action = stream ? "streamGenerateContent" : "generateContent";
        return base + "/v1beta/models/" + model + ":" + action + "?key=" + (apiKey != null ? apiKey : "");
    }

    private ChatResponse parseGeminiResponse(Map<String, Object> root, ChatRequest req, Instant t0) {
        String content = "";
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) root.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> c = (Map<String, Object>) candidates.get(0).get("content");
                if (c != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) c.get("parts");
                    if (parts != null) {
                        StringBuilder sb = new StringBuilder();
                        for (Map<String, Object> p : parts) {
                            if (p.get("text") != null) sb.append(p.get("text"));
                        }
                        content = sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Gemini parse content failed: {}", e.getMessage());
        }
        int pt = 0, ct = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) root.get("usageMetadata");
            if (usage != null) {
                pt = toInt(usage.get("promptTokenCount"));
                ct = toInt(usage.get("candidatesTokenCount"));
            }
        } catch (Exception ignore) {}
        return ChatResponse.builder()
                .id("gemini-" + System.currentTimeMillis())
                .model(req.getModel())
                .content(content)
                .promptTokens(pt).completionTokens(ct).totalTokens(pt + ct)
                .finishReason("stop")
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
