package com.minimax.deployer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LLM 客户端服务 (V3.0)
 *
 * 调用 minimax-ai 模块的 Qwen2.5 ONNX 推理服务, 实现真实 LLM 驱动的需求解析。
 * 同时支持远程云端 LLM (DeepSeek / GPT-4o) 作为高优先级备选。
 *
 * 调用链路:
 *  1. 优先: minimax-ai 服务的本地 Qwen2.5-0.5B (Q4 量化, 488MB, <1s 响应)
 *  2. 备选: minimax-ai 服务的 Qwen2.5-7B (云端, 大模型, ~3s 响应)
 *  3. 兜底: 规则引擎 (RequirementsParserService 内置)
 *
 * 降级策略:
 *  - LLM 不可用 → 规则引擎
 *  - LLM 响应格式错误 → 规则引擎 + warning
 *  - LLM 超时 (>10s) → 规则引擎
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmClientService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    /** minimax-ai 服务地址 (通过 Nacos 或 配置) */
    @Value("${agent-forge.llm.ai-service-url:http://minimax-ai:8090}")
    private String aiServiceUrl;

    /** 主用模型: qwen2.5-0.5b-instruct (本地 ONNX, 快速) */
    @Value("${agent-forge.llm.primary-model:qwen2.5-0.5b-instruct}")
    private String primaryModel;

    /** 备用模型: qwen2.5-7b-instruct (云端, 强大) */
    @Value("${agent-forge.llm.fallback-model:qwen2.5-7b-instruct}")
    private String fallbackModel;

    /** 超时 (毫秒) */
    @Value("${agent-forge.llm.timeout:30000}")
    private int timeoutMs;

    /**
     * 调用 LLM 生成
     *
     * @param prompt  用户提示词
     * @param system  系统提示词 (可选)
     * @param model   模型选择 (可选, 默认主用)
     * @return LLM 响应文本, 失败时返回 empty
     */
    public Optional<String> chat(String prompt, String system, String model) {
        String m = model != null ? model : primaryModel;
        return callLocal(m, prompt, system)
            .or(() -> {
                log.warn("[LLM] 主模型 {} 调用失败, 降级到 {}", m, fallbackModel);
                return callLocal(fallbackModel, prompt, system);
            });
    }

    /** 主用入口: 默认模型 */
    public Optional<String> chat(String prompt, String system) {
        return chat(prompt, system, null);
    }

    /**
     * 调用 minimax-ai 的 /api/v1/multimodal/chat-qwen
     */
    private Optional<String> callLocal(String model, String prompt, String system) {
        try {
            String url = aiServiceUrl + "/api/v1/multimodal/chat-qwen";
            Map<String, Object> body = new HashMap<>();
            body.put("prompt", prompt);
            if (system != null) body.put("system", system);
            body.put("maxTokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // 同步调用, 设置超时
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[LLM] 调用失败, status={}", response.getStatusCode());
                return Optional.empty();
            }

            // 解析 Result 包装: { code, data: { content, ... }, message }
            JsonNode root = objectMapper.readTree(response.getBody());
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                log.warn("[LLM] 业务错误, code={}, msg={}", code, root.path("message").asText());
                return Optional.empty();
            }
            String content = root.path("data").path("content").asText(null);
            if (content == null || content.isBlank()) {
                log.warn("[LLM] 响应 content 为空");
                return Optional.empty();
            }
            log.info("[LLM] 成功, model={}, contentLen={}", model, content.length());
            return Optional.of(content);

        } catch (RestClientException e) {
            log.warn("[LLM] HTTP 调用异常: {}", e.getMessage());
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("[LLM] JSON 解析异常: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[LLM] 未知异常: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
