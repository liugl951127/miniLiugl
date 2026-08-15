package com.minimax.multimodal.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * OpenAI 协议视觉模型提供者 (V3.0.1)
 *
 * <p>兼容 OpenAI Chat Completions API 的视觉模型:
 *   - OpenAI: gpt-4o / gpt-4-vision / gpt-4-turbo
 *   - 兼容协议: DeepSeek-VL / Qwen-VL / 智谱 GLM-4V / 任何 OpenAI 协议端点
 *
 * <p>请求格式 (OpenAI 协议):
 * <pre>
 * POST {baseUrl}/chat/completions
 * {
 *   "model": "gpt-4o",
 *   "messages": [{
 *     "role": "user",
 *     "content": [
 *       {"type": "text", "text": "..."},
 *       {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}}
 *     ]
 *   }]
 * }
 * </pre>
 */
@Slf4j
@Component
public class OpenAIVisionProvider implements MultimodalModelProvider {

    /** Provider 名 (registry 路由用) */
    @Override
    public String name() {
        return "openai";
    }

    @Override
    public String description() {
        return "OpenAI 协议视觉模型 (gpt-4o/4-vision, 兼容 DeepSeek-VL/Qwen-VL/GLM-4V)";
    }

    /** baseUrl 配置 (默认 OpenAI 官方) */
    @Value("${minimax.multimodal.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /** API key (从环境变量或配置中心读取) */
    @Value("${minimax.multimodal.openai.api-key:}")
    private String apiKey;

    /** 模型名 (gpt-4o / gpt-4-vision-preview) */
    @Value("${minimax.multimodal.openai.model:gpt-4o}")
    private String model;

    /** HTTP 超时 (秒) */
    @Value("${minimax.multimodal.openai.timeout-seconds:60}")
    private int timeoutSeconds;

    /** 最大返回 token 数 */
    @Value("${minimax.multimodal.openai.max-tokens:1000}")
    private int maxTokens;

    /** HTTP 客户端 (Java 11+ 内置, 无需引入 OkHttp) */
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))  // 连接超时 10s
            .build();

    /** JSON 序列化/反序列化 */
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 是否就绪: API key 必须非空
     */
    @Override
    public boolean isReady() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 调用 OpenAI 协议视觉 API
     */
    @Override
    public String describe(String imageBase64, String mimeType, String prompt) throws Exception {
        // 1. 检查就绪状态
        if (!isReady()) {
            throw new IllegalStateException("OpenAI provider 未配置 API key");
        }

        // 2. 构造 OpenAI 协议请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);  // 模型名

        // 3. 构造 messages[0].content 数组 (多模态内容)
        // 3a. 文本部分
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");                                          // 标记为文本
        textPart.put("text", prompt == null ? "请描述这张图片" : prompt);       // 文本内容

        // 3b. 图片部分 (data URL 形式: data:image/png;base64,xxxxx)
        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", "data:" + (mimeType == null ? "image/png" : mimeType)
                + ";base64," + imageBase64);  // data URL

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");           // 标记为图片 URL
        imagePart.put("image_url", imageUrlMap);      // URL 引用

        // 3c. 组合 message
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");                                          // 用户角色
        message.put("content", List.of(textPart, imagePart));                 // 多模态内容

        // 4. 完整请求体
        body.put("messages", List.of(message));
        body.put("max_tokens", maxTokens);  // 限制返回长度

        // 5. 构造 HTTP 请求
        String url = stripSlash(baseUrl) + "/chat/completions";  // 去掉 baseUrl 末尾的 /
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))                                              // URL
                .timeout(Duration.ofSeconds(timeoutSeconds))                       // 超时
                .header("Content-Type", "application/json")                       // JSON 头
                .header("Authorization", "Bearer " + apiKey)                       // Bearer Token
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))  // POST body
                .build();

        // 6. 发送请求
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        // 7. 错误处理
        if (resp.statusCode() >= 400) {
            // 4xx/5xx 抛异常, 调用方会降级
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
        }

        // 8. 解析响应
        @SuppressWarnings("unchecked")  // Jackson 泛型擦除
        Map<String, Object> result = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");

        // 9. 提取 content
        if (choices == null || choices.isEmpty()) {
            return "(无响应)";  // 空响应
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");  // 文本内容
    }

    /**
     * 委托给 inspect 工具
     */
    @Override
    public Map<String, Object> inspect(String imageBase64) {
        return ImageInspector.inspect(imageBase64);
    }

    /** 去掉字符串末尾的 / (避免 URL 双斜杠) */
    private String stripSlash(String s) {
        if (s == null) return "";                          // null 保护
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** 截断字符串 (用于错误信息) */
    private String truncate(String s, int n) {
        if (s == null) return null;                        // null 保护
        return s.length() > n ? s.substring(0, n) : s;    // 超长截断
    }
}
