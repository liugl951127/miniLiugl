package com.minimax.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 模块 → Model 模块 HTTP 客户端 (V7.0)
 *
 * 通过 RestTemplate 远程调用 minimax-model 服务的 /api/v1/models/chat 接口，
 * 替代直接注入 ModelService Bean（跨容器不可用）。
 */
@Slf4j
@Service
public class ModelClient {

    private final RestTemplate modelRestTemplate;

    @Value("${minimax.model.service-url:http://minimax-model:8084}")
    private String modelServiceUrl;

    public ModelClient() {
        this.modelRestTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(120_000))
                .build();
    }

    /**
     * 同步调用 Model 服务进行对话
     *
     * @param userId 用户ID
     * @param req    对话请求
     * @return ChatResponse，失败返回 null
     */
    public ChatResponse chat(Long userId, ChatRequest req) {
        try {
            String url = modelServiceUrl + "/api/v1/models/chat";
            log.debug("[ModelClient] 调用 Model 服务: url={}, model={}", url, req.getModel());

            // 构建请求体
            JSONObject body = JSONObject.from(req);

            // 发送 POST 请求
            String resp = modelRestTemplate.postForObject(url, body, String.class);
            if (resp == null) {
                log.warn("[ModelClient] Model 服务返回空");
                return null;
            }

            // 解析 Result<ChatResponse> 响应
            JSONObject result = JSON.parseObject(resp);
            if (result.getIntValue("code") != 0) {
                log.warn("[ModelClient] Model 服务调用失败: code={}, msg={}",
                        result.getIntValue("code"), result.getString("message"));
                return null;
            }

            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                log.warn("[ModelClient] Model 服务返回 data 为空");
                return null;
            }

            ChatResponse response = data.toJavaObject(ChatResponse.class);
            log.debug("[ModelClient] Model 服务调用成功, content长度={}",
                    response.getContent() != null ? response.getContent().length() : 0);
            return response;

        } catch (Exception e) {
            log.error("[ModelClient] Model 服务调用异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取所有已启用的模型列表（含自研 + 云端）。
     * 供 SmartModelRouter 构建候选模型池。
     */
    public List<Map<String, Object>> listEnabledModels() {
        try {
            String url = modelServiceUrl + "/api/v1/models/enabled";
            String resp = modelRestTemplate.getForObject(url, String.class);
            if (resp == null) return List.of();
            JSONObject result = JSON.parseObject(resp);
            if (result.getIntValue("code") != 0) return List.of();
            Object data = result.get("data");
            if (data instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?>) out.add((Map<String, Object>) item);
                }
                return out;
            }
        } catch (Exception e) {
            log.warn("[ModelClient] 获取启用模型列表失败: {}", e.getMessage());
        }
        return List.of();
    }
}
