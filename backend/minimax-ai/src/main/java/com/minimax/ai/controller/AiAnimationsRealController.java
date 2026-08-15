package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Animations 真实业务控制器 (V6.6+)
 * 动画效果: 文字淡入 / 进度条 / 加载
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/animation-impl")
@RequiredArgsConstructor
public class AiAnimationsRealController {

    /**
     * 文字淡入
     */
    @PostMapping("/text-fade")
    public Result<Map<String, Object>> textFade(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of(
            "type", "text-fade",
            "duration", body.getOrDefault("duration", 1000),
            "text", body.get("text"),
            "renderedAt", System.currentTimeMillis()
        ));
    }

    /**
     * 进度
     */
    @PostMapping("/progress")
    public Result<Map<String, Object>> progress(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of(
            "type", "progress",
            "current", body.getOrDefault("current", 0),
            "total", body.getOrDefault("total", 100),
            "percent", body.getOrDefault("percent", 0)
        ));
    }

    /**
     * 类型列表
     */
    @GetMapping("/types")
    public Result<List<String>> types() {
        return Result.ok(List.of("text-fade", "slide-in", "scale-up", "rotate", "progress"));
    }
}
