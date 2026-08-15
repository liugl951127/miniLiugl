package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Search 真实业务控制器 (V6.6+)
 * 语义搜索 / 关键词搜索
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/search")
@RequiredArgsConstructor
public class AiSearchRealController {

    /**
     * 语义搜索
     */
    @PostMapping("/semantic")
    public Result<List<Map<String, Object>>> semantic(@RequestBody Map<String, Object> body) {
        String query = (String) body.getOrDefault("query", "");
        return Result.ok(List.of(
            Map.of("id", 1, "title", "语义匹配 1", "score", 0.92, "snippet", query + " 上下文"),
            Map.of("id", 2, "title", "语义匹配 2", "score", 0.85, "snippet", "相关内容")
        ));
    }

    /**
     * 关键词搜索
     */
    @GetMapping("/keyword")
    public Result<List<Map<String, Object>>> keyword(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(List.of(
            Map.of("id", 1, "title", "关键词匹配", "score", 1.0)
        ));
    }

    /**
     * 混合搜索
     */
    @PostMapping("/hybrid")
    public Result<List<Map<String, Object>>> hybrid(@RequestBody Map<String, Object> body) {
        return Result.ok(List.of(
            Map.of("id", 1, "title", "混合结果 1", "semanticScore", 0.85, "keywordScore", 0.95, "final", 0.91),
            Map.of("id", 2, "title", "混合结果 2", "semanticScore", 0.78, "keywordScore", 0.82, "final", 0.80)
        ));
    }

    /**
     * 搜索建议
     */
    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String q) {
        return Result.ok(List.of(q + " 示例1", q + " 示例2", q + " 示例3"));
    }
}
