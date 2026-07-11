package com.minimax.ai.controller;

import com.minimax.ai.generation.ImageGenerator;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AIGC 图片生成 API (V2.7.5)
 *
 * 端点:
 *   POST /api/ai/image/generate  生成图片 (返回 base64)
 *   POST /api/ai/image/preview   生成图片 (返回 raw bytes)
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/image")
@RequiredArgsConstructor
public class ImageGenController {

    private final ImageGenerator imageGenerator;

    @PostMapping("/generate")
    public Result<ImageGenerator.ImageResult> generate(@RequestBody ImageGenerator.ImageRequest req) {
        try {
            return Result.ok(imageGenerator.generate(req));
        } catch (Exception e) {
            log.error("Image generation failed", e);
            return Result.fail("生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/types")
    public Result<String[]> types() {
        return Result.ok(new String[]{"abstract", "gradient", "pattern", "text", "scene", "logo", "infographic"});
    }

    @GetMapping("/infer")
    public Result<Map<String, Object>> infer(@RequestParam String prompt) {
        String type = imageGenerator.inferType(prompt);
        return Result.ok(Map.of("prompt", prompt, "type", type));
    }
}
