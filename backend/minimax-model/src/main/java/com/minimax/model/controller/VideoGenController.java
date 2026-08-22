package com.minimax.model.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 视频生成 (V6.8+)
 *
 * 支持 (按优先级):
 *   1. SiliconFlow (I2V 文生视频/图生视频)
 *   2. Mock - 返回 SVG 动画占位视频
 *
 * 端点:
 *   GET  /api/v1/video/models    列出可用模型
 *   POST /api/v1/video/generate  文生视频 (text → video)
 *   POST /api/v1/video/i2v       图生视频 (image + text → video)
 *
 * @since 2026-08
 */
@Slf4j
@Tag(name = "视频生成")
@RestController
@RequestMapping("/api/v1/video")
public class VideoGenController {

    @Value("${minimax.video.mock-mode:false}")
    private boolean mockMode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Operation(summary = "列出可用视频生成模型")
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> models() {
        List<Map<String, Object>> ms = new ArrayList<>();
        ms.add(Map.of(
                "code", "tencent/hunyuan-video",
                "displayName", "腾讯混元视频 (SiliconFlow)",
                "provider", "siliconflow",
                "type", "text-to-video",
                "maxDuration", "5s",
                "pricePerSec", 0,
                "enabled", true
        ));
        ms.add(Map.of(
                "code", "tencent/hunyuan-i2v",
                "displayName", "腾讯混元图生视频 (SiliconFlow)",
                "provider", "siliconflow",
                "type", "image-to-video",
                "maxDuration", "5s",
                "pricePerSec", 0,
                "enabled", true
        ));
        ms.add(Map.of(
                "code", "mock",
                "displayName", "Mock (沙箱演示, 返回彩色动画 GIF)",
                "provider", "mock",
                "type", "text-to-video",
                "maxDuration", "6s",
                "pricePerSec", 0,
                "enabled", true
        ));
        return Result.ok(ms);
    }

    @Operation(summary = "文生视频 (text → video)")
    @PostMapping("/generate")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> generate(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "model", defaultValue = "") String model,
            @RequestParam(value = "duration", defaultValue = "6") int duration,
            @RequestParam(value = "resolution", defaultValue = "768P") String resolution) {

        if (prompt == null || prompt.isBlank()) {
            return Result.fail(400, "prompt 不能为空");
        }

        long t0 = System.currentTimeMillis();
        try {
            if ("mock".equals(model) || mockMode) {
                return Result.ok(mockVideo(prompt, duration, t0));
            }

            Map<String, Object> out = realTextToVideo(model, prompt, duration, resolution, t0);
            return Result.ok(out);
        } catch (Exception e) {
            log.warn("video generate 失败: {}", e.getMessage());
            return Result.fail(500, "生成失败: " + e.getMessage());
        }
    }

    @Operation(summary = "图生视频 (image + text → video)")
    @PostMapping("/i2v")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> imageToVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", defaultValue = "") String prompt,
            @RequestParam(value = "model", defaultValue = "") String model,
            @RequestParam(value = "duration", defaultValue = "6") int duration) {

        long t0 = System.currentTimeMillis();
        try {
            if ("mock".equals(model) || mockMode) {
                return Result.ok(mockVideo("image-to-video: " + prompt, duration, t0));
            }

            Map<String, Object> out = realImageToVideo(model, file, prompt, duration, t0);
            return Result.ok(out);
        } catch (Exception e) {
            log.warn("video i2v 失败: {}", e.getMessage());
            return Result.fail(500, "生成失败: " + e.getMessage());
        }
    }

    // ====================== Mock ======================

    private Map<String, Object> mockVideo(String prompt, int duration, long t0) {
        String svg = buildAnimSvg(prompt, duration);
        String b64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        String dataUri = "data:image/svg+xml;base64," + b64;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("videoUrl", dataUri);
        out.put("thumbnailUrl", dataUri);
        out.put("prompt", prompt);
        out.put("model", "mock");
        out.put("duration", duration + "s");
        out.put("resolution", "mock");
        out.put("latencyMs", System.currentTimeMillis() - t0);
        out.put("mock", true);
        out.put("provider", "mock");
        out.put("note", "Mock 模式，请配置 SILICONFLOW_API_KEY 使用真实模型");
        return out;
    }

    private String buildAnimSvg(String prompt, int duration) {
        int w = 640, h = 360;
        long seed = prompt.hashCode() & 0xFFFFFFFFL;
        int r = (int) ((seed >> 16) & 0xFF);
        int g = (int) ((seed >> 8) & 0xFF);
        int b = (int) (seed & 0xFF);
        String c1 = String.format("#%02x%02x%02x", r, g, b);
        String c2 = String.format("#%02x%02x%02x", (r + 80) % 256, (g + 60) % 256, (b + 40) % 256);
        String safe = prompt.length() > 40 ? prompt.substring(0, 40) + "..." : prompt;
        safe = safe.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 " + w + " " + h + "' width='" + w + "' height='" + h + "'>"
                + "<defs>"
                + "<linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0' stop-color='" + c1 + "'/>"
                + "<stop offset='1' stop-color='" + c2 + "'/>"
                + "<animate attributeName='x1' values='0;1;0' dur='" + (duration * 2) + "s' repeatCount='indefinite'/>"
                + "</linearGradient>"
                + "</defs>"
                + "<rect width='" + w + "' height='" + h + "' fill='url(#g)'/>"
                + "<text x='" + (w / 2) + "' y='" + (h / 2 - 10) + "' font-size='28' fill='white' text-anchor='middle' font-family='sans-serif' opacity='0.9'>" + safe + "</text>"
                + "<text x='" + (w / 2) + "' y='" + (h / 2 + 25) + "' font-size='16' fill='white' text-anchor='middle' font-family='sans-serif' opacity='0.6'>[Mock 视频演示]</text>"
                + "</svg>";
    }

    // ====================== SiliconFlow ======================

    @SuppressWarnings("unchecked")
    private Map<String, Object> realTextToVideo(String model, String prompt, int duration, String resolution, long t0) {
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("需要 SILICONFLOW_API_KEY 环境变量，或用 model=mock");
        }
        String endpoint = "https://api.siliconflow.cn/v1/video/submit";

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("prompt", prompt);
        req.put("duration", duration);

        try {
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req)))
                    .build();
            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resp.body(), Map.class);
            String taskId = (String) body.getOrDefault("task_id", "");

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("taskId", taskId);
            out.put("videoUrl", "https://api.siliconflow.cn/v1/video/" + taskId);
            out.put("prompt", prompt);
            out.put("model", model);
            out.put("duration", duration + "s");
            out.put("status", "processing");
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            out.put("provider", "siliconflow");
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> realImageToVideo(String model, MultipartFile file, String prompt, int duration, long t0) {
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("需要 SILICONFLOW_API_KEY 环境变量");
        }
        String endpoint = "https://api.siliconflow.cn/v1/video/submit";

        try {
            byte[] imgBytes = file.getBytes();
            String imgB64 = Base64.getEncoder().encodeToString(imgBytes);
            String boundary = "----Minimax" + System.currentTimeMillis();

            StringBuilder sb = new StringBuilder();
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n").append(model).append("\r\n");
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"image\"\r\n\r\n").append("data:image/png;base64,").append(imgB64).append("\r\n");
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n").append(prompt).append("\r\n");
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"duration\"\r\n\r\n").append(duration).append("\r\n");
            sb.append("--").append(boundary).append("--\r\n");

            byte[] bodyBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();
            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resp.body(), Map.class);
            String taskId = (String) body.getOrDefault("task_id", "");

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("taskId", taskId);
            out.put("videoUrl", "https://api.siliconflow.cn/v1/video/" + taskId);
            out.put("prompt", prompt);
            out.put("model", model);
            out.put("filename", file.getOriginalFilename());
            out.put("duration", duration + "s");
            out.put("status", "processing");
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            out.put("provider", "siliconflow");
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
