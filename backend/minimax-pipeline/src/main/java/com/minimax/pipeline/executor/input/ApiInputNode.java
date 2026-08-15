package com.minimax.pipeline.executor.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * API INPUT 节点 (V5.32) - 调 HTTP API
 *
 * config: {
 *   url: "https://api.example.com/users",
 *   method: "GET" | "POST",     // 默认 GET
 *   headers: { "Authorization": "Bearer xxx" },
 *   body: {...},                 // POST 用
 *   dataPath: "data.items"       // JSONPath, 可选, 提取子数组
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiInputNode extends NodeExecutor {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public NodeType supportedType() { return NodeType.API_INPUT; }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        HttpRequest.Builder hb = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(30));
        Map<String, String> headers = (Map<String, String>) config.get("headers");
        if (headers != null) headers.forEach(hb::header);
        if ("POST".equalsIgnoreCase(method)) {
            String body = json.writeValueAsString(config.get("body"));
            hb.POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        } else {
            hb.GET();
        }
        log.info("[{}] API input: {} {}", nodeId, method, url);
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("API " + resp.statusCode() + ": " + resp.body());
        }
        Object parsed = json.readValue(resp.body(), Object.class);
        // 可选 dataPath 提取
        String dataPath = (String) config.get("dataPath");
        if (dataPath != null) {
            for (String seg : dataPath.split("\\.")) {
                if (parsed instanceof Map) parsed = ((Map<String, Object>) parsed).get(seg);
                else if (parsed instanceof List && seg.matches("\\d+")) parsed = ((List<?>) parsed).get(Integer.parseInt(seg));
                else break;
            }
        }
        if (parsed instanceof List) return (List<Map<String, Object>>) parsed;
        if (parsed instanceof Map) return List.of((Map<String, Object>) parsed);
        return List.of();
    }
}
