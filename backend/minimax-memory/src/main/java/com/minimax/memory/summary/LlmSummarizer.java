package com.minimax.memory.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.memory.context.ContextBuilder;
import com.minimax.memory.shortterm.ShortTermMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 真实 LLM 摘要 (Day 7 升级版)。
 *
 * 替换 Day 6 的"截前 60 字符"占位实现。
 * 调 model 服务的 /models/chat (OpenAI 兼容)。
 *
 * 用法：
 *  1. 取会话最近的 N 条消息
 *  2. 拼成 messages 数组
 *  3. 调 model 服务生成摘要
 *  4. 返回摘要文本
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmSummarizer {

    @Value("${minimax.memory.summary.model:MiniMax-Text-01}")
    private String model;

    @Value("${minimax.memory.summary.base-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${minimax.memory.summary.token:}")
    private String token;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ShortTermMemory memory;

    /**
     * 给 sessionId 的最近 N 条消息做 LLM 摘要。
     * @return 摘要文本（≤ 200 字）。失败时降级到 Day 6 的截前 60 字符实现。
     */
    public String summarize(Long sessionId, int recent) {
        List<Map<String, String>> msgs = memory.recent(sessionId, recent);
        if (msgs.isEmpty()) return null;

        String transcript = msgs.stream()
                .map(m -> "[" + m.getOrDefault("role", "?") + "] " + truncate(m.getOrDefault("content", ""), 200))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // 1) 先试真实 LLM
        try {
            String prompt = "请用 100-200 字客观总结以下对话的关键信息（不含客套）：\n\n" + transcript;
            String result = callChat(prompt);
            if (result != null && !result.isBlank()) {
                log.info("LLM 摘要成功: sessionId={} len={}", sessionId, result.length());
                return result;
            }
        } catch (Exception e) {
            log.warn("LLM 摘要失败，降级截前 60 字符: {}", e.getMessage());
        }
        // 2) 降级
        return fallbackSummary(msgs);
    }

    private String callChat(String userPrompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role","system","content","你是一个对话摘要助手。"),
                Map.of("role","user","content", userPrompt)
        ));
        body.put("temperature", 0.3);
        body.put("max_tokens", 500);

        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(baseUrl) + "/api/v1/models/chat"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (token != null && !token.isBlank()) {
            hb.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + resp.body());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return data == null ? null : (String) data.get("content");
    }

    private String fallbackSummary(List<Map<String, String>> msgs) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> m : msgs) {
            String c = m.getOrDefault("content", "");
            if (c.length() > 60) c = c.substring(0, 60) + "...";
            sb.append("[").append(m.getOrDefault("role", "?")).append("] ").append(c).append("; ");
        }
        return sb.toString();
    }

    private String truncate(String s, int n) {
        return s == null ? "" : (s.length() > n ? s.substring(0, n) + "..." : s);
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s);
    }
}
