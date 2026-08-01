package com.bank.dualrecord.quality.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 通义千问 LLM Provider(阿里云 DashScope)
 *
 * <p>API: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 * <p>兼容 OpenAI 接口
 */
@Slf4j
@Component
public class QwenProvider implements LlmProvider {

    @Value("${llm.qwen.api-key:}")
    private String apiKey;

    @Value("${llm.qwen.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String endpoint;

    @Value("${llm.qwen.model:qwen-max}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.1
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("Qwen API 失败: " + resp.statusCode() + " " + resp.body());
            }
            Map<String, Object> result = objectMapper.readValue(resp.body(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Qwen 调用失败", e);
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void streamComplete(String systemPrompt, String userPrompt, StreamCallback callback) {
        // 简化:同步调用 + 一次性返回
        try {
            String result = complete(systemPrompt, userPrompt);
            callback.onChunk(result);
            callback.onComplete(result);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public List<Map<String, Object>> functionCall(String systemPrompt, String userPrompt, List<Map<String, Object>> tools) {
        throw new UnsupportedOperationException("暂未实现");
    }

    @Override
    public boolean healthCheck() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getId() {
        return "qwen";
    }
}
