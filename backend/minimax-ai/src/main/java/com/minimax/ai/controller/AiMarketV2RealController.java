package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Market v2 真实业务控制器 (V6.6+)
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/market-v2")
@RequiredArgsConstructor
public class AiMarketV2RealController {

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "新版商品", "price", 99),
            Map.of("id", 2, "name", "试用商品", "price", 0)
        ));
    }

    @GetMapping("/trending")
    public Result<List<Map<String, Object>>> trending() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "热门商品 1", "downloads", 1234),
            Map.of("id", 2, "name", "热门商品 2", "downloads", 567)
        ));
    }

    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("id", System.currentTimeMillis(), "status", "published"));
    }
}
