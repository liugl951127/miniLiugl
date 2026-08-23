package com.minimax.deployer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * LLM 客户端 (V4.0)
 *
 * V4.0 简化: V3.0 那个 LlmClientService 过度设计 (3 层 fallback + 5 个模型选择)
 * V4.0 只保留: 1 个方法 chat(prompt, system), 1 个默认模型, 1 次 HTTP 调用
 *
 * 调用 minimax-ai 服务的 /api/v1/multimodal/chat-qwen 端点
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agent-forge.llm.url:http://minimax-ai:8090}")
    private String aiUrl;

    @Value("${agent-forge.llm.model:qwen2.5-0.5b-instruct}")
    private String model;

    @Value("${agent-forge.llm.timeout-ms:30000}")
    private int timeoutMs;

    /**
     * 调 LLM
     * @return 响应内容, 失败时 empty (不抛异常)
     */
    public Optional<String> chat(String prompt, String system) {
        try {
            String url = aiUrl + "/api/v1/multimodal/chat-qwen";
            Map<String, Object> body = Map.of(
                "prompt", prompt,
                "system", system != null ? system : "",
                "maxTokens", 2048
            );
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, h), String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return Optional.empty();
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.path("code").asInt(-1) != 0) return Optional.empty();
            String content = root.path("data").path("content").asText(null);
            return Optional.ofNullable(content).filter(s -> !s.isBlank());
        } catch (Exception e) {
            log.warn("[LLM] 调用失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public String getModel() { return model; }
}
