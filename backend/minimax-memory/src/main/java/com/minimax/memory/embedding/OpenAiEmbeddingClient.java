package com.minimax.memory.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * 调 OpenAI 兼容的 /embeddings 端点。
 * 适用于：OpenAI text-embedding-3-small / MiniMax-Embedding / Ollama / 智谱。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "minimax.memory.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    @Value("${minimax.memory.embedding.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${minimax.memory.embedding.api-key:}")
    private String apiKey;

    @Value("${minimax.memory.embedding.model:text-embedding-3-small}")
    private String model;

    @Value("${minimax.memory.embedding.dim:1536}")
    private int vectorDim;

    @Override
    public String code() { return "openai"; }

    @Override
    public int dim() { return vectorDim; }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return new float[vectorDim];
        try {
            ObjectMapper json = new ObjectMapper();
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", text);

            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(baseUrl) + "/embeddings"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (apiKey != null && !apiKey.isBlank()) {
                hb.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(hb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " " + resp.body());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = json.readValue(resp.body(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            if (data == null || data.isEmpty()) return new float[vectorDim];
            @SuppressWarnings("unchecked")
            List<Number> vec = (List<Number>) data.get(0).get("embedding");
            float[] out = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) out[i] = vec.get(i).floatValue();
            return out;
        } catch (Exception e) {
            log.error("Embedding 失败: {}", e.getMessage());
            // 失败时返回零向量（不抛异常，业务降级）
            return new float[vectorDim];
        }
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s);
    }
}
