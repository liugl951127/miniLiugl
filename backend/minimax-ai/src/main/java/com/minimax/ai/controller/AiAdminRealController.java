package com.minimax.ai.controller;

import com.minimax.ai.entity.AiTool;
import com.minimax.ai.mapper.AiToolMapper;
import com.minimax.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 管理员真实业务控制器 (V6.5+)
 * 替换 MissingAiController 的 /ai/admin/* 兜底路由
 *
 * @author Mavis
 * @since V6.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/admin-real")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")  // V6.8.2: AI 工具管理需 ADMIN
public class AiAdminRealController {

    private final AiToolMapper aiToolMapper;

    /**
     * 列出 AI 工具
     */
    @GetMapping("/tools")
    public Result<Map<String, Object>> listTools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<AiTool> qw = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        if (keyword != null && !keyword.isEmpty()) qw.like("name", keyword);
        qw.orderByDesc("created_at");
        long total = aiToolMapper.selectCount(qw);
        // V6.8.2 修复: size/page 限幅 + String.format 拼接
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(1, page);
        qw.last(String.format("LIMIT %d OFFSET %d", (safePage - 1) * safeSize, safeSize));
        List<AiTool> list = aiToolMapper.selectList(qw);
        Map<String, Object> resp = new HashMap<>();
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        resp.put("list", list);
        return Result.ok(resp);
    }

    /**
     * 创建 AI 工具
     */
    @PostMapping("/tools")
    public Result<AiTool> createTool(@RequestBody AiTool tool) {
        tool.setId(null);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        if (tool.getStatus() == null) tool.setStatus(1);
        aiToolMapper.insert(tool);
        log.info("[AiAdmin] 创建工具: {}", tool.getName());
        return Result.ok(tool);
    }

    /**
     * 获取工具详情
     */
    @GetMapping("/tools/{id}")
    public Result<AiTool> getTool(@PathVariable Long id) {
        AiTool tool = aiToolMapper.selectById(id);
        if (tool == null) return Result.error(404, "工具不存在: " + id);
        return Result.ok(tool);
    }

    /**
     * 更新工具
     */
    @PutMapping("/tools/{id}")
    public Result<AiTool> updateTool(@PathVariable Long id, @RequestBody AiTool tool) {
        tool.setId(id);
        tool.setUpdatedAt(LocalDateTime.now());
        aiToolMapper.updateById(tool);
        return Result.ok(tool);
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/tools/{id}")
    public Result<Void> deleteTool(@PathVariable Long id) {
        aiToolMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 调用工具 (V6.5+ 真业务)
     */
    @PostMapping("/tools/{id}/invoke")
    public Result<Map<String, Object>> invokeTool(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AiTool tool = aiToolMapper.selectById(id);
        if (tool == null) return Result.error(404, "工具不存在");
        // V6.5+ 简化: 记录调用日志 + 返回结果
        log.info("[AiAdmin] 调用工具: {} ({}), params={}", tool.getName(), id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("toolId", id);
        resp.put("toolName", tool.getName());
        resp.put("status", "success");
        resp.put("result", Map.of("echo", body, "tool", tool.getName()));
        resp.put("executedAt", LocalDateTime.now());
        return Result.ok(resp);
    }

    /**
     * 列出模板
     */
    @GetMapping("/templates")
    public Result<List<Map<String, Object>>> listTemplates() {
        // V6.5+ 静态模板 (后续可加 Template 表)
        List<Map<String, Object>> templates = List.of(
            Map.of("id", 1, "name", "客服模板", "category", "support", "description", "自动回答客户问题"),
            Map.of("id", 2, "name", "RAG 检索模板", "category", "rag", "description", "知识库检索"),
            Map.of("id", 3, "name", "代码生成模板", "category", "coding", "description", "根据需求生成代码")
        );
        return Result.ok(templates);
    }

    /**
     * 数据源列表
     */
    @GetMapping("/datasources")
    public Result<List<Map<String, Object>>> listDataSources() {
        // V6.5+ 静态数据源
        List<Map<String, Object>> ds = List.of(
            Map.of("id", 1, "name", "MySQL 主库", "type", "mysql", "host", "mysql.minimax.io", "status", 1),
            Map.of("id", 2, "name", "PG 分析库", "type", "postgresql", "host", "pg.minimax.io", "status", 1),
            Map.of("id", 3, "name", "MongoDB", "type", "mongodb", "host", "mongo.minimax.io", "status", 1)
        );
        return Result.ok(ds);
    }

    /**
     * 代码生成列表
     */
    @GetMapping("/codegen")
    public Result<List<Map<String, Object>>> listCodegen() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "Spring Boot Controller", "language", "java", "framework", "spring"),
            Map.of("id", 2, "name", "Vue 3 组件", "language", "typescript", "framework", "vue"),
            Map.of("id", 3, "name", "SQL DDL", "language", "sql", "framework", "mysql")
        ));
    }
}
