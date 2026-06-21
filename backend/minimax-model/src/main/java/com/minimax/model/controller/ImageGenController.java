package com.minimax.model.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 文生图 (V4.1).
 *
 * 支持 (按优先级):
 *   1. SiliconFlow (siliconflow.cn) - 国产 SDXL / FLUX / Kolors
 *   2. DashScope (阿里 wanx) - 通义万相
 *   3. OpenAI DALL-E 3 - 如果有 key
 *   4. Mock - 返回占位图 (data URI)
 *
 * 端点:
 *   GET  /api/v1/imagegen/models    列出可用模型
 *   POST /api/v1/imagegen/generate  生成 (返回 base64 / url)
 *
 * @since 2026-06
 */
@Slf4j
@Tag(name = "模型管理")
@RestController
@RequestMapping("/imagegen")
@RequiredArgsConstructor
public class ImageGenController {

    @Value("${minimax.imagegen.mock-mode:true}")
    private boolean mockMode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Operation(summary = "列出可用文生图模型")
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> models() {
        List<Map<String, Object>> ms = new ArrayList<>();
        // SiliconFlow
        ms.add(Map.of(
                "code", "black-forest-labs/FLUX.1-schnell",
                "displayName", "FLUX.1 Schnell (硅基流动)",
                "provider", "siliconflow",
                "size", "1024x1024",
                "pricePerImg", 0,
                "enabled", true
        ));
        ms.add(Map.of(
                "code", "stabilityai/stable-diffusion-xl-base-1.0",
                "displayName", "SDXL 1.0 (硅基流动)",
                "provider", "siliconflow",
                "size", "1024x1024",
                "pricePerImg", 0,
                "enabled", true
        ));
        ms.add(Map.of(
                "code", "Kwai-Kolors/Kolors",
                "displayName", "快手 Kolors (硅基流动)",
                "provider", "siliconflow",
                "size", "1024x1024",
                "pricePerImg", 0,
                "enabled", true
        ));
        // DashScope 通义万相
        ms.add(Map.of(
                "code", "wanx-v1",
                "displayName", "通义万相 v1 (DashScope)",
                "provider", "dashscope",
                "size", "1024x1024",
                "pricePerImg", 0,
                "enabled", true
        ));
        // OpenAI DALL-E
        ms.add(Map.of(
                "code", "dall-e-3",
                "displayName", "DALL-E 3 (OpenAI)",
                "provider", "openai",
                "size", "1024x1024",
                "pricePerImg", 0.04,
                "enabled", true
        ));
        // Mock
        ms.add(Map.of(
                "code", "mock",
                "displayName", "Mock (沙箱演示, SVG 渐变)",
                "provider", "mock",
                "size", "1024x1024",
                "pricePerImg", 0,
                "enabled", true
        ));
        return Result.ok(ms);
    }

    @Operation(summary = "生成图片")
    @PostMapping("/generate")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String prompt = (String) body.getOrDefault("prompt", "");
        String model = (String) body.getOrDefault("model", "mock");
        String size = (String) body.getOrDefault("size", "1024x1024");
        Integer n = body.get("n") != null ? ((Number) body.get("n")).intValue() : 1;
        boolean returnBase64 = Boolean.TRUE.equals(body.get("base64"));

        if (prompt.isBlank()) {
            return Result.fail(400, "prompt 不能为空");
        }

        long t0 = System.currentTimeMillis();
        try {
            if ("mock".equals(model) || mockMode) {
                // SVG 渐变占位图 (1x1 PNG), 实际是 data URI 字符串
                List<String> images = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    images.add(buildMockSvg(prompt, i, size));
                }
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("model", model);
                out.put("prompt", prompt);
                out.put("size", size);
                out.put("count", images.size());
                out.put("images", images);  // data:image/svg+xml;base64,...
                out.put("latencyMs", System.currentTimeMillis() - t0);
                out.put("mock", true);
                out.put("provider", "mock");
                return Result.ok(out);
            }

            // 真实调用 (siliconflow / openai / dashscope)
            Map<String, Object> out = realCall(model, prompt, size, n, t0);
            return Result.ok(out);
        } catch (Exception e) {
            log.warn("imagegen 失败: {}", e.getMessage());
            return Result.fail(500, "生成失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> realCall(String model, String prompt, String size, int n, long t0) {
        // 简化: 直接转给 SiliconFlow (OpenAI 协议 image generation)
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("需要 SILICONFLOW_API_KEY 环境变量, 或用 model=mock");
        }
        String endpoint = "https://api.siliconflow.cn/v1/images/generations";

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("prompt", prompt);
        req.put("image_size", size);
        req.put("num_images_per_prompt", n);
        req.put("response_format", "b64_json");

        try {
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req)))
                    .build();
            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " " + resp.body());
            }
            Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resp.body(), Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.getOrDefault("data", List.of());
            List<String> images = new ArrayList<>();
            for (Map<String, Object> d : dataList) {
                String b64 = (String) d.get("b64_json");
                if (b64 != null) images.add("data:image/png;base64," + b64);
                else if (d.get("url") != null) images.add((String) d.get("url"));
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("model", model);
            out.put("prompt", prompt);
            out.put("size", size);
            out.put("count", images.size());
            out.put("images", images);
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            out.put("provider", "siliconflow");
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成 SVG 渐变占位图 (data URI).
     * 用 prompt 前 12 字符做"伪哈希"决定颜色.
     */
    private String buildMockSvg(String prompt, int seed, String size) {
        String[] parts = size.split("x");
        int w = parts.length > 0 ? Integer.parseInt(parts[0]) : 1024;
        int h = parts.length > 1 ? Integer.parseInt(parts[1]) : 1024;

        // 颜色: 用 prompt hash
        long h1 = 0, h2 = 0;
        for (int i = 0; i < prompt.length(); i++) {
            h1 = (h1 * 31 + prompt.charAt(i) + seed) & 0xFFFFFFL;
            h2 = (h2 * 37 + prompt.charAt(prompt.length() - 1 - i) - seed) & 0xFFFFFFL;
        }
        String c1 = String.format("#%06x", h1);
        String c2 = String.format("#%06x", h2);

        // 文字 (UTF-8 escape)
        String safePrompt = prompt.length() > 60 ? prompt.substring(0, 60) + "..." : prompt;
        safePrompt = safePrompt.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 " + w + " " + h + "'>"
                + "<defs>"
                + "<linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0' stop-color='" + c1 + "'/>"
                + "<stop offset='1' stop-color='" + c2 + "'/>"
                + "</linearGradient>"
                + "</defs>"
                + "<rect width='" + w + "' height='" + h + "' fill='url(#g)'/>"
                + "<text x='" + (w / 2) + "' y='" + (h / 2) + "' font-size='48' "
                + "fill='white' text-anchor='middle' font-family='sans-serif' "
                + "font-weight='bold' opacity='0.85'>"
                + safePrompt + "</text>"
                + "<text x='" + (w / 2) + "' y='" + (h / 2 + 60) + "' font-size='24' "
                + "fill='white' text-anchor='middle' font-family='sans-serif' opacity='0.6'>"
                + "[Mock 沙箱演示, 配 key 后用真实模型]</text>"
                + "</svg>";

        String b64 = Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + b64;
    }
}
