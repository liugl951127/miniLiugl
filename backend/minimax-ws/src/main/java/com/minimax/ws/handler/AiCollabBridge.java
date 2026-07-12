package com.minimax.ws.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 协作桥接器 (V2.8.8)
 *
 * <p>把协作房间的 AI 请求转发到 minimax-ai 模块的 13 阶段 Pipeline.</p>
 *
 * <h3>调用方式</h3>
 * <ul>
 *   <li>HTTP 模式: POST {minimax-ai}/api/v1/pipeline/execute</li>
 *   <li>本地模式: 注入 PipelineExecutor 直接调用 (更优, 减少网络)</li>
 * </ul>
 *
 * <h3>设计</h3>
 * <p>用 Spring {@code @Autowired(required = false)} 软依赖,
 * minimax-ai 未启动时 fallback 到 mock (V2.8.7 行为).</p>
 *
 * @author MiniMax
 * @since V2.8.8
 */
@Slf4j
@Component
public class AiCollabBridge {

    @Value("${minimax.ai.url:http://localhost:8094}")
    private String aiBaseUrl;

    @Value("${minimax.ai.enabled:false}")
    private boolean aiEnabled;

    @Autowired(required = false)
    private RestClient restClient;

    /**
     * 调用 minimax-ai Pipeline 生成 AI 回复
     *
     * @param roomId   协作房间 ID (作为 sessionId)
     * @param userId   用户 ID
     * @param username 用户名
     * @param prompt   用户输入
     * @return AI 回复内容
     */
    public String generate(String roomId, Long userId, String username, String prompt) {
        if (!aiEnabled) {
            throw new IllegalStateException("AI Pipeline 未启用 (minimax.ai.enabled=false)");
        }
        if (restClient == null) {
            restClient = RestClient.create();
        }
        try {
            // 调用 minimax-ai 的 Pipeline API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", prompt);
            requestBody.put("userId", userId);
            requestBody.put("sessionId", "collab-" + roomId);
            requestBody.put("model", "minimax-7b");
            // 上下文
            Map<String, Object> context = new HashMap<>();
            context.put("source", "collab");
            context.put("username", username);
            requestBody.put("context", context);

            Map<String, Object> response = restClient.post()
                .uri(aiBaseUrl + "/api/v1/pipeline/execute")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                throw new RuntimeException("AI Pipeline 返回 null");
            }
            // 取 response.data.output
            Object data = response.get("data");
            if (data instanceof Map) {
                Object output = ((Map<?, ?>) data).get("output");
                if (output != null) {
                    log.info("[ai-bridge] 协作 AI 成功 roomId={} userId={} chars={}",
                        roomId, userId, output.toString().length());
                    return output.toString();
                }
            }
            // fallback: 整 response 转字符串
            return response.toString();
        } catch (Exception e) {
            log.warn("[ai-bridge] AI Pipeline 调用失败: {}", e.getMessage());
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        return aiEnabled;
    }
}
