package com.minimax.multimodal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.common.result.Result;
import com.minimax.multimodal.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 多模态控制器 (Day 11).
 *
 *   POST /multimodal/upload         上传图片 (返回 base64 + 基础信息)
 *   POST /multimodal/describe       文字 + 图片 (base64) → 描述
 *   POST /multimodal/chat           流式文字 + 图片对话
 *   GET  /multimodal/info           当前视觉模型信息
 */
@Tag(name = "多模态")
@RestController
@RequestMapping("/multimodal")
@RequiredArgsConstructor
public class MultimodalController {

    private final VisionService vision;

    @Operation(summary = "上传图片（返回 base64 + 元信息）")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new IllegalArgumentException("文件为空");
        byte[] bytes = file.getBytes();
        if (bytes.length > 20 * 1024 * 1024) throw new IllegalArgumentException("文件超过 20MB");
        String mime = file.getContentType() == null ? "image/png" : file.getContentType();
        String b64 = Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> info = vision.inspect(b64);

        Map<String, Object> data = new HashMap<>();
        data.put("filename", file.getOriginalFilename());
        data.put("mimeType", mime);
        data.put("sizeBytes", bytes.length);
        data.put("sizeKB", bytes.length / 1024);
        data.put("base64", b64);
        data.put("info", info);
        data.put("dataUrl", "data:" + mime + ";base64," + b64);
        return Result.ok(data);
    }

    @Operation(summary = "图片理解（文字+图片→描述）")
    @PostMapping("/describe")
    public Result<Map<String, Object>> describe(@RequestBody Map<String, String> body) {
        String b64 = body.get("imageBase64");
        String mime = body.getOrDefault("mimeType", "image/png");
        String prompt = body.getOrDefault("prompt", "请描述这张图片");
        long t0 = System.currentTimeMillis();
        String description = vision.describe(b64, mime, prompt);
        long dur = System.currentTimeMillis() - t0;

        Map<String, Object> r = new HashMap<>();
        r.put("description", description);
        r.put("durationMs", dur);
        r.put("prompt", prompt);
        r.put("info", vision.inspect(b64));
        return Result.ok(r);
    }

    @Operation(summary = "多模态模型信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> r = new HashMap<>();
        r.put("model", "gpt-4o (mock)");
        r.put("provider", "openai");
        r.put("mockMode", true);
        r.put("supportedFormats", java.util.List.of("png", "jpeg", "gif", "webp"));
        r.put("maxSize", "20MB");
        return Result.ok(r);
    }
}
