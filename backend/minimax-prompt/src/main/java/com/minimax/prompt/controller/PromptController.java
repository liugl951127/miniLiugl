package com.minimax.prompt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minimax.common.result.Result;
import com.minimax.prompt.entity.PromptTemplate;
import com.minimax.prompt.service.PromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Prompt 模板 HTTP 端点.
 * V4.3 新增 — 8091 端口
 *
 * 端点:
 *   GET    /prompts              分页列表 (支持分类+搜索过滤)
 *   GET    /prompts/{id}         模板详情
 *   POST   /prompts              创建模板
 *   PUT    /prompts/{id}         更新模板 (仅创建者)
 *   DELETE /prompts/{id}         删除模板 (仅创建者，软删)
 *   POST   /prompts/{id}/use     使用计数 +1
 *   GET    /prompts/categories   全部分类列表
 *   POST   /prompts/resolve      变量解析 (填值生成最终 prompt)
 */
@Tag(name = "Prompt模板")
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptTemplateService promptService;

    @PostConstruct
    public void init() {
        // 启动时初始化 5 个内置模板
        promptService.initBuiltin();
    }

    // ---------- 查询 ----------

    @Operation(summary = "分页查询Prompt模板")
    @GetMapping
    public Result<IPage<PromptTemplate>> list(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.ok(promptService.page(userId, current, size, category, keyword));
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public Result<PromptTemplate> get(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        PromptTemplate t = promptService.getById(id, userId);
        if (t == null) return Result.fail("模板不存在或无权访问");
        return Result.ok(t);
    }

    @Operation(summary = "获取所有分类")
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(promptService.categories());
    }

    // ---------- 写操作 ----------

    @Operation(summary = "创建Prompt模板")
    @PostMapping
    public Result<PromptTemplate> create(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @AuthenticationPrincipal(expression = "username") String userName,
            @RequestBody PromptTemplate template) {
        return Result.ok(promptService.create(template, userId, userName));
    }

    @Operation(summary = "更新Prompt模板")
    @PutMapping("/{id}")
    public Result<PromptTemplate> update(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestBody PromptTemplate updates) {
        try {
            return Result.ok(promptService.update(id, updates, userId));
        } catch (SecurityException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "删除Prompt模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        try {
            boolean ok = promptService.delete(id, userId);
            return ok ? Result.ok(null) : Result.fail("删除失败");
        } catch (SecurityException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "使用计数+1")
    @PostMapping("/{id}/use")
    public Result<Void> use(@PathVariable Long id) {
        promptService.incrementUseCount(id);
        return Result.ok(null);
    }

    @Operation(summary = "变量解析生成最终Prompt")
    @PostMapping("/resolve")
    public Result<String> resolve(@RequestBody ResolveRequest req) {
        if (req.getContent() == null || req.getValues() == null) {
            return Result.fail("content 和 values 不能为空");
        }
        String resolved = promptService.resolve(req.getContent(), req.getValues());
        return Result.ok(resolved);
    }

    @Data
    public static class ResolveRequest {
        private String content;
        private Map<String, String> values;
    }
}