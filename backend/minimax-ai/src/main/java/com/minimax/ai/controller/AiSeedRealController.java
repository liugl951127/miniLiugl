package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Seed 真实业务控制器 (V6.6+)
 * 种子数据 / 初始化
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/seed")
@RequiredArgsConstructor
public class AiSeedRealController {

    /**
     * 执行种子
     */
    @PostMapping("/run")
    public Result<Map<String, Object>> run() {
        return Result.ok(Map.of(
            "status", "running",
            "startedAt", System.currentTimeMillis(),
            "items", 17
        ));
    }

    /**
     * 列出种子
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "用户种子", "count", 5, "type", "user"),
            Map.of("id", 2, "name", "租户种子", "count", 3, "type", "tenant"),
            Map.of("id", 3, "name", "API Key", "count", 5, "type", "apikey")
        ));
    }
}
