package com.minimax.multimodal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视觉模型服务。
 *
 * 协议: OpenAI Chat Completions with image_url content part
 *   messages: [
 *     {role: "user", content: [
 *       {type: "text", text: "..."},
 *       {type: "image_url", image_url: {url: "data:image/png;base64,..."}}
 *     ]}
 *   ]
 *
 * 模型: gpt-4o / gpt-4-vision / MiniMax-VL-01
 * Mock 模式: 返回固定中文描述
 */
@Slf4j
@Service
public class VisionService {

    @Value("${minimax.multimodal.provider:openai}")
    private String provider;

    @Value("${minimax.multimodal.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${minimax.multimodal.api-key:}")
    private String apiKey;

    @Value("${minimax.multimodal.model:gpt-4o}")
    private String model;

    @Value("${minimax.multimodal.timeout-seconds:60}")
    private int timeout;

    @Value("${minimax.multimodal.mock-mode:true}")
    private boolean mockMode;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 单图理解。
     * @param imageBase64  原始图片 base64 (无 data: 前缀)
     * @param mimeType     image/png / image/jpeg
     * @param prompt       用户文字提示
     */
    public String describe(String imageBase64, String mimeType, String prompt) {
        if (mockMode || apiKey == null || apiKey.isBlank()) {
            return mockDescribe(imageBase64, mimeType, prompt);
        }
        try {
            return callVisionApi(imageBase64, mimeType, prompt);
        } catch (Exception e) {
            log.warn("视觉 API 失败, 降级 mock: {}", e.getMessage());
            return mockDescribe(imageBase64, mimeType, prompt);
        }
    }

    private String mockDescribe(String b64, String mime, String prompt) {
        // 简单 mock: 基于 base64 长度 + mime 推测
        int approxBytes = b64 == null ? 0 : (b64.length() * 3 / 4);
        String size;
        if (approxBytes < 50_000) size = "小";
        else if (approxBytes < 500_000) size = "中等";
        else size = "大";
        return String.format(
            "【视觉模型 Mock 模式】\n" +
            "类型: %s\n" +
            "尺寸: %s (约 %d KB)\n" +
            "提示: %s\n\n" +
            "由于未配置真实视觉 API (minimax.multimodal.api-key), 这是 mock 响应。\n" +
            "请设置 minimax.multimodal.api-key + minimax.multimodal.mock-mode=false 以使用真实模型。",
            mime == null ? "image/unknown" : mime,
            size,
            approxBytes / 1024,
            prompt == null ? "(无提示)" : prompt
        );
    }

    private String callVisionApi(String imageBase64, String mimeType, String prompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt == null ? "请描述这张图片" : prompt);

        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", "data:" + (mimeType == null ? "image/png" : mimeType) +
                ";base64," + imageBase64);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlMap);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textPart, imagePart));

        body.put("messages", List.of(message));
        body.put("max_tokens", 1000);

        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(baseUrl) + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (apiKey != null && !apiKey.isBlank()) {
            hb.header("Authorization", "Bearer " + apiKey);
        }
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
        if (choices == null || choices.isEmpty()) return "(无响应)";
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }

    /**
     * 多图理解 (e.g. 对比两张图)
     */
    public String describeMulti(List<Map<String, String>> images, String prompt) {
        // 简化: 拼接图片一起调
        if (images == null || images.isEmpty()) {
            return mockDescribe(null, null, prompt);
        }
        Map<String, String> first = images.get(0);
        return describe(first.get("base64"), first.get("mimeType"), prompt);
    }

    /** 简单图片信息 (从 base64 前 16 字节 magic number) */
    public Map<String, Object> inspect(String imageBase64) {
        Map<String, Object> info = new HashMap<>();
        if (imageBase64 == null || imageBase64.isBlank()) {
            info.put("error", "empty");
            return info;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64.substring(0, Math.min(64, imageBase64.length())));
            String magic = new String(bytes, 0, Math.min(8, bytes.length));
            String format;
            if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P') format = "png";
            else if (bytes.length >= 2 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xD8) format = "jpeg";
            else if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') format = "gif";
            else if (bytes.length >= 4 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F') format = "webp";
            else format = "unknown";
            info.put("format", format);
            info.put("magic", magic.replaceAll("[^\\x20-\\x7E]", "?"));
            info.put("approxBytes", imageBase64.length() * 3 / 4);
            info.put("approxKB", (imageBase64.length() * 3 / 4) / 1024);
        } catch (Exception e) {
            info.put("error", "decode failed: " + e.getMessage());
        }
        return info;
    }

    private String stripSlash(String s) { return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s); }
    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
