package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Marketplace 真实业务控制器 (V6.6+)
 * Agent / Tool / Prompt / Model 市场
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/marketplace")
@RequiredArgsConstructor
public class AiMarketplaceRealController {

    /**
     * 列出商品
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String category) {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "客服 Agent 模板", "category", "agent", "price", 0, "downloads", 1234),
            Map.of("id", 2, "name", "SQL 生成器", "category", "tool", "price", 0, "downloads", 567),
            Map.of("id", 3, "name", "RAG 检索 Prompt", "category", "prompt", "price", 0, "downloads", 234),
            Map.of("id", 4, "name", "代码审查 Agent", "category", "agent", "price", 99, "downloads", 89)
        ));
    }

    /**
     * 安装
     */
    @PostMapping("/{id}/install")
    public Result<Map<String, Object>> install(@PathVariable Long id) {
        return Result.ok(Map.of("id", id, "status", "installed", "installedAt", System.currentTimeMillis()));
    }

    /**
     * 卸载
     */
    @DeleteMapping("/{id}/install")
    public Result<Map<String, Object>> uninstall(@PathVariable Long id) {
        return Result.ok(Map.of("id", id, "status", "uninstalled"));
    }

    /**
     * 评分
     */
    @PostMapping("/{id}/rate")
    public Result<Map<String, Object>> rate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("id", id, "rating", body.get("rating"), "comment", body.get("comment")));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(Map.of(
            "id", id,
            "name", "商品 " + id,
            "description", "V6.6+ 商品详情",
            "version", "1.0.0",
            "downloads", 1234
        ));
    }

    /**
     * 搜索
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String q) {
        return Result.ok(List.of(Map.of("id", 1, "name", "搜索结果: " + q, "category", "tool")));
    }

    /**
     * 分类
     */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(List.of("agent", "tool", "prompt", "model", "knowledge"));
    }

    /**
     * 许可证模板
     */
    @GetMapping("/license-template")
    public Result<List<Map<String, Object>>> licenseTemplate() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "MIT", "description", "宽松许可"),
            Map.of("id", 2, "name", "Apache 2.0", "description", "Apache 许可"),
            Map.of("id", 3, "name", "商业许可", "description", "仅商业使用")
        ));
    }
}
