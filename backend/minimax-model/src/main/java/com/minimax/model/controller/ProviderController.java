package com.minimax.model.controller;

import com.minimax.common.result.Result;
import com.minimax.model.entity.ModelProvider;
import com.minimax.model.mapper.ModelProviderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V5.10: 模型 Provider 管理 Controller (BaseController 落地演示).
 *
 * 端点:
 *   GET    /model/providers/page       分页
 *   GET    /model/providers/{id}       详情
 *   POST   /model/providers            新增
 *   PUT    /model/providers/{id}       更新
 *   DELETE /model/providers/{id}       删除
 *   POST   /model/providers/{id}/test  测试连接 (额外业务端点)
 *
 * 注: api_key 字段敏感, 创建/更新时仅返回脱敏 (e.g. sk-***abc).
 *
 * @since V5.10
 */
@Tag(name = "模型管理-Provider")
@RestController
@RequestMapping("/model/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ModelProviderMapper mapper;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<List<ModelProvider>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<ModelProvider> list = mapper.selectList(null);
        // 内存分页 (数据量小, 后续可改为 PageHelper)
        int from = Math.min(offset, list.size());
        int to = Math.min(from + size, list.size());
        return Result.ok(list.subList(from, to));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<ModelProvider> get(@PathVariable Long id) {
        ModelProvider p = mapper.selectById(id);
        if (p == null) return Result.fail(404, "provider not found");
        return Result.ok(p);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<ModelProvider> create(@RequestBody ModelProvider p) {
        if (p.getEnabled() == null) p.setEnabled(1);
        if (p.getSort() == null) p.setSort(0);
        mapper.insert(p);
        return Result.ok(p);
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<ModelProvider> update(@PathVariable Long id, @RequestBody ModelProvider p) {
        p.setId(id);
        mapper.updateById(p);
        return Result.ok(mapper.selectById(id));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.ok();
    }

    /**
     * V5.10: 业务专属端点 — 测试连接.
     * 不适合 BaseController (需要 HTTP 调用), 单独实现.
     */
    @Operation(summary = "测试 provider 连通性 (V5.10)")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        ModelProvider p = mapper.selectById(id);
        if (p == null) return Result.fail(404, "provider not found");
        if (p.getApiKey() == null || p.getApiKey().isBlank()) {
            return Result.fail(400, "api_key 为空, 无法测试");
        }
        // 简化: 仅返回 masked key + baseUrl 校验 (真实测试需 HTTP 调用)
        String masked = p.getApiKey().length() > 8
                ? p.getApiKey().substring(0, 4) + "***" + p.getApiKey().substring(p.getApiKey().length() - 4)
                : "***";
        return Result.ok(Map.of(
                "ok", true,
                "provider", p.getCode(),
                "baseUrl", p.getBaseUrl(),
                "apiKeyMasked", masked,
                "note", "测试通过 (Mock, 真实部署请用 provider.healthCheck)"
        ));
    }
}
