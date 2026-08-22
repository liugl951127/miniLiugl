package com.minimax.pipeline.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.common.result.Result;
import com.minimax.pipeline.entity.RuleDefinition;
import com.minimax.pipeline.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 规则定义 Controller (T1-backend-apis / P0)
 *
 * 5 个端点:
 * <ul>
 *   <li>POST   /api/v1/rule       创建规则</li>
 *   <li>GET    /api/v1/rule       列表 (分页)</li>
 *   <li>GET    /api/v1/rule/{id}  详情</li>
 *   <li>PUT    /api/v1/rule/{id}  更新</li>
 *   <li>DELETE /api/v1/rule/{id}  软删</li>
 * </ul>
 *
 * 前端对接: views/rule/Index.vue saveRule() / deleteRule()
 *
 * @since V7.2
 */
@Tag(name = "规则定义 (V7.2 P0)")
@RestController
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @Operation(summary = "创建规则")
    @PostMapping
    public Result<Long> create(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String json = (String) body.get("json");
        String scope = (String) body.get("scope");
        Integer enabled = toInt(body.get("enabled"));
        return Result.ok(ruleService.create(name, json, scope, enabled, userId));
    }

    @Operation(summary = "规则列表 (分页)")
    @GetMapping
    public Result<Page<RuleDefinition>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(ruleService.list(page, size, keyword));
    }

    @Operation(summary = "规则详情")
    @GetMapping("/{id}")
    public Result<RuleDefinition> getById(@PathVariable Long id) {
        return Result.ok(ruleService.getById(id));
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String json = (String) body.get("json");
        String scope = (String) body.get("scope");
        Integer enabled = toInt(body.get("enabled"));
        ruleService.update(id, name, json, scope, enabled, userId);
        return Result.ok();
    }

    @Operation(summary = "软删规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        ruleService.delete(id, userId);
        return Result.ok();
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }
}
