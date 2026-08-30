package com.minimax.pipeline.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.common.result.Result;
import com.minimax.pipeline.dto.RuleUpsertRequest;
import com.minimax.pipeline.entity.RuleDefinition;
import com.minimax.pipeline.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
@Slf4j
@Tag(name = "规则定义 (V7.2 P0)")
@RestController
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;
    private final com.minimax.pipeline.service.RuleAiService ruleAiService;  // V9.1

    @Operation(summary = "创建规则")
    @PostMapping
    public Result<Long> create(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                @Valid @RequestBody RuleUpsertRequest req) {
        return Result.ok(ruleService.create(
                req.getName(), req.getJson(), req.getScope(),
                req.getEnabled(), userId));
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

    @Operation(summary = "更新规则 (部分字段允许为 null 表示不更新)")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                @Valid @RequestBody RuleUpsertRequest req) {
        ruleService.update(
                id, req.getName(), req.getJson(), req.getScope(),
                req.getEnabled(), userId);
        return Result.ok();
    }

    @Operation(summary = "软删规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        ruleService.delete(id, userId);
        return Result.ok();
    }

    /**
     * 把任意对象转 Integer, 容错: 解析失败时打 log.warn 而不是静默吞错
     * (T3-new-code-robustness: 替换原 catch (Exception e) { return null; })
     */
    @SuppressWarnings("unused")
    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            log.warn("[Rule] toInt 解析失败, value={}", o);
            return null;
        }
    }

    /**
     * 把任意对象转 BigDecimal, 容错: 解析失败时打 log.warn (T3-new-code-robustness)
     */
    @SuppressWarnings("unused")
    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            log.warn("[Rule] toBigDecimal 解析失败, value={}", o);
            return null;
        }
    }

    /** V9.1: AI 生成规则 (自然语言 → 规则 JSON) */
    @Operation(summary = "自然语言生成规则 (V9.1)")
    @PostMapping("/ai-generate")
    public Result<com.minimax.pipeline.service.RuleAiService.AiGenResult> aiGenerate(
            @RequestBody Map<String, String> body) {
        return Result.ok(ruleAiService.generate(body.get("text")));
    }
}
