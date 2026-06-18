package com.minimax.prompt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minimax.common.result.Result;
import com.minimax.prompt.entity.PromptTemplate;
import com.minimax.prompt.service.PromptTemplateService;
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
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptTemplateService promptService;

    @PostConstruct
    public void init() {
        // 启动时初始化 5 个内置模板
        promptService.initBuiltin();
    }

    // ---------- 查询 ----------

    @GetMapping
    public Result<IPage<PromptTemplate>> list(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.success(promptService.page(userId, current, size, category, keyword));
    }

    @GetMapping("/{id}")
    public Result<PromptTemplate> get(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        PromptTemplate t = promptService.getById(id, userId);
        if (t == null) return Result.fail("模板不存在或无权访问");
        return Result.success(t);
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(promptService.categories());
    }

    // ---------- 写操作 ----------

    @PostMapping
    public Result<PromptTemplate> create(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @AuthenticationPrincipal(expression = "username") String userName,
            @RequestBody PromptTemplate template) {
        return Result.success(promptService.create(template, userId, userName));
    }

    @PutMapping("/{id}")
    public Result<PromptTemplate> update(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestBody PromptTemplate updates) {
        try {
            return Result.success(promptService.update(id, updates, userId));
        } catch (SecurityException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        try {
            boolean ok = promptService.delete(id, userId);
            return ok ? Result.success(null) : Result.fail("删除失败");
        } catch (SecurityException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/use")
    public Result<Void> use(@PathVariable Long id) {
        promptService.incrementUseCount(id);
        return Result.success(null);
    }

    /** 变量解析接口：填入变量值，返回最终 prompt */
    @PostMapping("/resolve")
    public Result<String> resolve(@RequestBody ResolveRequest req) {
        if (req.getContent() == null || req.getValues() == null) {
            return Result.fail("content 和 values 不能为空");
        }
        String resolved = promptService.resolve(req.getContent(), req.getValues());
        return Result.success(resolved);
    }

    @Data
    public static class ResolveRequest {
        private String content;
        private Map<String, String> values;
    }
}
