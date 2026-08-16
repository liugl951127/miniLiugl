package com.minimax.model.provider;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;

/**
 * 自研 ONNX 模型适配器 (V7.0 — 真实推理)
 *
 * <h3>职责</h3>
 * 通过 HTTP 调用 minimax-ai 服务的 /api/v1/ai/chat/onnx/generate 端点，
 * 触发真实的 ONNX Runtime 推理。不再依赖 reflection 或 mock。
 *
 * <h3>配置</h3>
 * <pre>
 * minimax.model.ai-service-url: http://localhost:8094   (minimax-ai 服务地址)
 * minimax.onnx.enabled: true
 * minimax.onnx.model-dir: /workspace/onnx-models
 * minimax.onnx.model-name: mini-transformer
 * </pre>
 *
 * <h3>约定</h3>
 * endpoint = 模型文件所在目录路径，apiKey = 模型名称（不含 .onnx 后缀）。
 * 实际模型路径 = endpoint + "/" + apiKey + ".onnx"
 */
@Slf4j
@Component
public class OnnxLLMAdapter implements ModelProviderAdapter {

    /** minimax-ai 服务地址 */
    @Value("${minimax.model.ai-service-url:http://localhost:8094}")
    private String aiServiceUrl;

    /** HTTP client (长连接) */
    private final RestTemplate restTemplate;

    /** 已加载的模型路径缓存 */
    private final Set<String> loadedModels = ConcurrentHashMap.newKeySet();

    public OnnxLLMAdapter() {
        this.restTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMinutes(3))
                .build();
    }

    @Override
    public String code() { return "onnx"; }

    @Override
    public ChatResponse chat(String endpoint, String apiKey, ChatRequest req) {
        long start = System.currentTimeMillis();
        String modelPath = resolveModelPath(endpoint, apiKey);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", buildPrompt(req.getMessages()));
            body.put("modelPath", modelPath);
            body.put("temperature", getTemperature(req));
            body.put("maxTokens", getMaxTokens(req));
            body.put("topP", 0.9);

            String url = aiServiceUrl + "/api/v1/ai/chat/onnx/generate";
            log.debug("[OnnxAdapter] POST {} modelPath={}", url, modelPath);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, body, Map.class);

            if (resp == null) {
                return buildError("ONNX 服务返回空响应", start);
            }

            // 检查业务错误
            if (resp.containsKey("code") && !resp.get("code").equals(200)) {
                return buildError("ONNX 推理失败: " + resp.get("message"), start);
            }

            String text = (String) resp.getOrDefault("text", "");
            Number promptTokens = toNumber(resp.get("promptTokens"));
            Number completionTokens = toNumber(resp.get("completionTokens"));
            Number totalTokens = toNumber(resp.get("totalTokens"));

            log.info("[OnnxAdapter] 生成完成: path={}, tokens={}, latency={}ms",
                    modelPath, completionTokens, System.currentTimeMillis() - start);

            return ChatResponse.builder()
                    .model(req.getModel())
                    .content(text)
                    .promptTokens(promptTokens.intValue())
                    .completionTokens(completionTokens.intValue())
                    .totalTokens(totalTokens.intValue())
                    .finishReason("stop")
                    .latencyMs(System.currentTimeMillis() - start)
                    .providerCode(code())
                    .build();

        } catch (Exception e) {
            log.error("[OnnxAdapter] HTTP 调用失败: {} → {}", modelPath, e.getMessage());
            return buildError("ONNX 推理异常: " + e.getMessage(), start);
        }
    }

    @Override
    public OpenAiCompatibleAdapter.StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                                   Consumer<String> chunkJsonConsumer,
                                   AtomicBoolean stopFlag) {
        long start = System.currentTimeMillis();
        String modelPath = resolveModelPath(endpoint, apiKey);
        String prompt = buildPrompt(req.getMessages());
        double temperature = getTemperature(req);
        int maxTokens = getMaxTokens(req);

        // 流式: 调用同步接口，每次吐出一个字符
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", prompt);
            body.put("modelPath", modelPath);
            body.put("temperature", temperature);
            body.put("maxTokens", maxTokens);
            body.put("topP", 0.9);

            String url = aiServiceUrl + "/api/v1/ai/chat/onnx/generate";
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, body, Map.class);

            if (resp == null || resp.containsKey("code") && !resp.get("code").equals(200)) {
                String err = resp != null ? String.valueOf(resp.get("message")) : "空响应";
                chunkJsonConsumer.accept("{\"error\":\"" + escapeJson(err) + "\"}");
                return new OpenAiCompatibleAdapter.StreamResult(null, null, "", 0, 0, 0, "error", System.currentTimeMillis() - start);
            }

            String text = (String) resp.getOrDefault("text", "");
            int promptTokens = toNumber(resp.get("promptTokens")).intValue();
            int completionTokens = 0;

            for (int i = 0; i < text.length(); i++) {
                if (stopFlag.get()) break;
                char c = text.charAt(i);
                completionTokens++;
                String chunk = "{\"choices\":[{\"delta\":{\"content\":\"" + escapeJson(String.valueOf(c)) + "\"},\"index\":0}]}";
                chunkJsonConsumer.accept(chunk);
            }

            return new OpenAiCompatibleAdapter.StreamResult(null, null, text,
                    promptTokens, completionTokens, promptTokens + completionTokens,
                    "stop", System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("[OnnxAdapter/stream] 失败: {}", e.getMessage());
            chunkJsonConsumer.accept("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            return new OpenAiCompatibleAdapter.StreamResult(null, null, "", 0, 0, 0, "error", System.currentTimeMillis() - start);
        }
    }

    // ========== Prompt 构建 ==========

    private String buildPrompt(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) return "";
        return messages.stream()
                .filter(m -> m.get("role") != null)
                .map(m -> {
                    String role = (String) m.get("role");
                    Object c = m.get("content");
                    String content = c != null ? c.toString() : "";
                    if ("system".equals(role)) return "系统: " + content;
                    if ("user".equals(role)) return "用户: " + content;
                    if ("assistant".equals(role)) return "助手: " + content;
                    return content;
                })
                .collect(java.util.stream.Collectors.joining("\n"))
                + "\n助手: ";
    }

    private double getTemperature(ChatRequest req) {
        return req.getTemperature() != null ? req.getTemperature() : 0.7;
    }

    private int getMaxTokens(ChatRequest req) {
        if (req.getMaxTokens() != null && req.getMaxTokens() > 0) {
            return Math.min(req.getMaxTokens(), 2048);
        }
        return 512;
    }

    // ========== 模型路径解析 ==========

    private String resolveModelPath(String endpoint, String apiKey) {
        if (endpoint == null) return apiKey;
        endpoint = endpoint.trim();
        if (endpoint.endsWith(".onnx")) return endpoint;
        if (apiKey != null && !apiKey.isBlank()) {
            String base = endpoint.replaceAll("/+$", "");
            if (apiKey.endsWith(".onnx")) return base + "/" + apiKey;
            return base + "/" + apiKey + ".onnx";
        }
        return endpoint;
    }

    // ========== 工具 ==========

    private Number toNumber(Object v) {
        if (v instanceof Number) return (Number) v;
        if (v instanceof String && ((String) v).matches("\\d+")) return Integer.parseInt((String) v);
        return 0;
    }

    private ChatResponse buildError(String msg, long start) {
        return ChatResponse.builder()
                .content(msg)
                .finishReason("error")
                .latencyMs(System.currentTimeMillis() - start)
                .providerCode(code())
                .build();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        return Flux.error(new UnsupportedOperationException(
                "ONNX stream 通过 streamChat() 调用"));
    }

    @Override
    public boolean ping(String endpoint, String apiKey) {
        try {
            String url = aiServiceUrl + "/api/v1/ai/chat/onnx/status";
            var resp = restTemplate.getForObject(url, Map.class);
            return resp != null && Boolean.TRUE.equals(resp.get("enabled"));
        } catch (Exception e) {
            log.warn("[OnnxAdapter/ping] 失败: {}", e.getMessage());
            return false;
        }
    }
}
