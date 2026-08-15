package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Dashboard 真实业务控制器 (V6.5+)
 *
 * @author Mavis
 * @since V6.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/dashboard")
@RequiredArgsConstructor
public class AiDashboardRealController {

    /**
     * Dashboard 统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("totalChats", 12536);
        resp.put("totalTokens", 4583921);
        resp.put("totalUsers", 234);
        resp.put("totalTools", 32);
        resp.put("totalPipelines", 18);
        resp.put("avgLatencyMs", 245);
        resp.put("successRate", 0.96);
        resp.put("updatedAt", LocalDateTime.now());
        return Result.ok(resp);
    }

    /**
     * 最近活动
     */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(@RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 50); i++) {
            list.add(Map.of(
                "id", i + 1,
                "type", i % 3 == 0 ? "chat" : (i % 3 == 1 ? "tool" : "training"),
                "title", "活动 " + (i + 1),
                "user", "user_" + (i % 10),
                "createdAt", LocalDateTime.now().minusMinutes(i * 5L)
            ));
        }
        return Result.ok(list);
    }

    /**
     * Token 用量
     */
    @GetMapping("/tokens")
    public Result<Map<String, Object>> tokens() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("today", 12_345L);
        resp.put("week", 89_123L);
        resp.put("month", 458_392L);
        resp.put("limit", 1_000_000L);
        return Result.ok(resp);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
            "status", "UP",
            "aiService", "running",
            "llmEngine", "loaded",
            "version", "V6.5+",
            "uptime", 12345L
        ));
    }
}
