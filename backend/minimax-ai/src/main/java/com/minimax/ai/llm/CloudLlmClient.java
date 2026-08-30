package com.minimax.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云端 LLM 客户端 (V9.0) — 通用 OpenAI 兼容协议
 *
 * 支持: OpenAI / DeepSeek / 月之暗面 / 智谱 等 OpenAI 兼容 API
 * 通过配置 base-url 切换
 *
 * 关键: 这个组件 fail-fast, 任何异常都抛出去, 由 LlmGatewayService 决定是否兜底
 */
@Component
@Slf4j
public class CloudLlmClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${agent-forge.llm.cloud.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${agent-forge.llm.cloud.api-key:}")
    private String apiKey;

    @Value("${agent-forge.llm.cloud.timeout-ms:8000}")
    private int timeoutMs;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(List<Map<String, String>> messages, String model, int timeoutMs) {
        if (!isConfigured()) {
            throw new IllegalStateException("云端 LLM 未配置 api-key");
        }
        String url = baseUrl + "/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 2048);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, h), Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("云端返回 " + resp.getStatusCode());
            }
            var choices = (List<?>) resp.getBody().get("choices");
            if (choices == null || choices.isEmpty()) throw new RuntimeException("云端返回 choices 为空");
            var first = (Map<?, ?>) choices.get(0);
            var msg = (Map<?, ?>) first.get("message");
            if (msg == null) throw new RuntimeException("云端返回 message 为空");
            return (String) msg.get("content");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 网络/超时异常
            throw new RuntimeException("云端网络超时: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx/5xx
            throw new RuntimeException("云端 " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }
}
