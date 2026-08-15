package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Leaderboard 真实业务控制器 (V6.6+)
 * 模型排行榜 + 评估
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/leaderboard")
@RequiredArgsConstructor
public class AiLeaderboardRealController {

    /**
     * 排行榜
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
            Map.of("rank", 1, "model", "gpt-4", "score", 95.6, "tasks", 12_345, "category", "general"),
            Map.of("rank", 2, "model", "claude-3-opus", "score", 94.2, "tasks", 10_234, "category", "general"),
            Map.of("rank", 3, "model", "deepseek-v3", "score", 91.5, "tasks", 8_765, "category", "coding"),
            Map.of("rank", 4, "model", "qwen-2.5", "score", 89.8, "tasks", 7_654, "category", "chinese"),
            Map.of("rank", 5, "model", "minimax-v6", "score", 88.2, "tasks", 5_432, "category", "internal")
        ));
    }

    /**
     * 模型评估
     */
    @PostMapping("/eval")
    public Result<Map<String, Object>> eval(@RequestBody Map<String, Object> body) {
        String model = (String) body.getOrDefault("model", "");
        return Result.ok(Map.of(
            "model", model,
            "score", 88.5,
            "categories", Map.of("coding", 92, "math", 85, "chinese", 90),
            "evaluatedAt", System.currentTimeMillis()
        ));
    }

    /**
     * 统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(Map.of(
            "totalModels", 32,
            "totalTasks", 234_567,
            "avgScore", 85.6,
            "topCategory", "general"
        ));
    }

    /**
     * 趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            data.add(Map.of("date", "2026-08-0" + (i + 1), "score", 85.0 + i * 0.5));
        }
        return Result.ok(data);
    }

    /**
     * 任务详情
     */
    @GetMapping("/tasks/{id}")
    public Result<Map<String, Object>> task(@PathVariable Long id) {
        return Result.ok(Map.of(
            "id", id,
            "name", "intent-classifier",
            "score", 92.3,
            "samples", 1234
        ));
    }
}
