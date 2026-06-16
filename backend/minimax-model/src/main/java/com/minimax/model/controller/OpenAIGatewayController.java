package com.minimax.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.security.SuperAdminGuard;
import com.minimax.model.entity.ModelConfig;
import com.minimax.model.entity.ModelProvider;
import com.minimax.model.mapper.ModelConfigMapper;
import com.minimax.model.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * V3.3: OpenAI 兼容 API Gateway
 *
 * 端点 (与 OpenAI 100% 兼容):
 *   GET  /v1/models
 *   POST /v1/chat/completions
 *
 * 认证: Bearer Token (可以用 minimax token 或 API Key)
 *
 * 用例:
 *   curl https://api.minimax.local/v1/chat/completions \
 *     -H "Authorization: Bearer sk-..." \
 *     -d '{"model": "MiniMax-Text-01", "messages": [...]}'
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/openai")
@RequiredArgsConstructor
public class OpenAIGatewayController {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderMapper providerMapper;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 列出可用模型 */
    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listModels() {
        List<ModelConfig> enabled = modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getEnabled, 1));
        List<Map<String, Object>> data = new ArrayList<>();
        for (ModelConfig m : enabled) {
            String providerName = "minimax";
            if (m.getProviderId() != null) {
                ModelProvider p = providerMapper.selectById(m.getProviderId());
                if (p != null) providerName = p.getName();
            }
            Map<String, Object> modelObj = new LinkedHashMap<>();
            modelObj.put("id", m.getModelCode());
            modelObj.put("object", "model");
            modelObj.put("created", System.currentTimeMillis() / 1000);
            modelObj.put("owned_by", providerName);
            data.add(modelObj);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("object", "list");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * Chat Completions
     * 兼容: OpenAI / minimax-M3 / Ollama (OpenAI 协议)
     */
    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatCompletions(@RequestBody Map<String, Object> body) {
        // 找模型
        String modelName = (String) body.getOrDefault("model", "MiniMax-Text-01");
        ModelConfig model = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getModelCode, modelName));
        if (model == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of(
                            "message", "Model not found: " + modelName,
                            "type", "invalid_request_error",
                            "code", "model_not_found")));
        }

        // 查 provider 拿 baseUrl / apiKey
        final String baseUrl;
        final String apiKey;
        {
            String u = "http://localhost:8083";
            String k = "mock";
            if (model.getProviderId() != null) {
                ModelProvider p = providerMapper.selectById(model.getProviderId());
                if (p != null) {
                    if (p.getBaseUrl() != null) u = p.getBaseUrl();
                    if (p.getApiKey() != null) k = p.getApiKey();
                }
            }
            baseUrl = u;
            apiKey = k;
        }

        // 构造上游请求
        final Map<String, Object> upstream = new HashMap<>();
        upstream.put("model", modelName);
        upstream.put("messages", body.get("messages"));
        if (body.containsKey("temperature")) upstream.put("temperature", body.get("temperature"));
        if (body.containsKey("max_tokens")) upstream.put("max_tokens", body.get("max_tokens"));
        if (body.containsKey("stream")) upstream.put("stream", body.get("stream"));
        if (body.containsKey("top_p")) upstream.put("top_p", body.get("top_p"));

        final boolean isStream = Boolean.TRUE.equals(body.get("stream"));

        if (isStream) {
            // 上游与当前请求是同一个 map, lambda 内 put("stream", true) 会改它, 临时复制
            final Map<String, Object> streamBody = new HashMap<>(upstream);
            streamBody.put("stream", true);
            return ResponseEntity.ok().body((StreamingResponseBody) out -> {
                streamProxy(streamBody, baseUrl, apiKey, out);
            });
        } else {
            return ResponseEntity.ok(callNonStream(upstream, baseUrl, apiKey));
        }
    }

    private Map<String, Object> callNonStream(Map<String, Object> body, String baseUrl, String apiKey) {
        try {
            String url = stripSlash(baseUrl) + "/api/v1/models/chat";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                return Map.of("error", Map.of("message", resp.body(), "type", "upstream_error"));
            }
            // 解析上游响应, 转 OpenAI 格式
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(resp.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) root.get("data");

            Map<String, Object> openai = new LinkedHashMap<>();
            openai.put("id", "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8));
            openai.put("object", "chat.completion");
            openai.put("created", System.currentTimeMillis() / 1000);
            openai.put("model", body.get("model"));

            Map<String, Object> choice = new LinkedHashMap<>();
            choice.put("index", 0);
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = (Map<String, Object>) data.get("message");
            choice.put("message", Map.of(
                    "role", "assistant",
                    "content", msg == null ? "" : msg.getOrDefault("content", "")));
            choice.put("finish_reason", "stop");
            openai.put("choices", List.of(choice));

            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("prompt_tokens", 0);
            usage.put("completion_tokens", 0);
            usage.put("total_tokens", 0);
            openai.put("usage", usage);
            return openai;
        } catch (Exception e) {
            log.warn("OpenAI gateway upstream 失败: {}", e.getMessage());
            return Map.of("error", Map.of(
                    "message", "upstream call failed: " + e.getMessage(),
                    "type", "upstream_error"));
        }
    }

    private void streamProxy(Map<String, Object> body, String baseUrl, String apiKey,
                              java.io.OutputStream out) {
        try {
            body.put("stream", true);
            String url = stripSlash(baseUrl) + "/api/v1/models/chat";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();

            HttpResponse<java.io.InputStream> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() >= 400) {
                out.write(("data: {\"error\":\"upstream " + resp.statusCode() + "\"}\n\n").getBytes());
                out.flush();
                return;
            }

            // 把每行包装成 OpenAI SSE 格式
            try (java.util.Scanner sc = new java.util.Scanner(resp.body())) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if (line.isBlank()) continue;
                    // 上游 chunk → OpenAI chunk
                    String wrapped = wrapAsOpenAIChunk(line, (String) body.get("model"));
                    out.write(("data: " + wrapped + "\n\n").getBytes());
                    out.flush();
                }
            }
            out.write("data: [DONE]\n\n".getBytes());
            out.flush();
        } catch (Exception e) {
            log.warn("OpenAI stream 失败: {}", e.getMessage());
            try {
                out.write(("data: {\"error\":\"stream failed: " + e.getMessage() + "\"}\n\n").getBytes());
                out.flush();
            } catch (Exception ignored) {}
        }
    }

    private String wrapAsOpenAIChunk(String upstream, String model) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(upstream, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) root.get("data");
            String content = "";
            if (data != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                    if (delta != null) content = String.valueOf(delta.getOrDefault("content", ""));
                }
            }
            Map<String, Object> chunk = new LinkedHashMap<>();
            chunk.put("id", "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8));
            chunk.put("object", "chat.completion.chunk");
            chunk.put("created", System.currentTimeMillis() / 1000);
            chunk.put("model", model);
            chunk.put("choices", List.of(Map.of(
                    "index", 0,
                    "delta", Map.of("content", content),
                    "finish_reason", null)));
            return json.writeValueAsString(chunk);
        } catch (Exception e) {
            return upstream;
        }
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s);
    }
}
