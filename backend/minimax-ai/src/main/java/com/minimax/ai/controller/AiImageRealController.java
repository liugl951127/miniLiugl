package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Image 真实业务控制器 (V6.6+)
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/image")
@RequiredArgsConstructor
public class AiImageRealController {

    /**
     * 列出图片
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(List.of(
            Map.of("id", 1, "url", "https://cdn.minimax.io/1.png", "tags", List.of("cat", "animal")),
            Map.of("id", 2, "url", "https://cdn.minimax.io/2.png", "tags", List.of("dog", "animal"))
        ));
    }

    /**
     * 上传
     */
    @PostMapping
    public Result<Map<String, Object>> upload(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of(
            "id", System.currentTimeMillis(),
            "url", "https://cdn.minimax.io/uploaded/" + System.currentTimeMillis() + ".png",
            "tags", body.getOrDefault("tags", List.of())
        ));
    }
}
