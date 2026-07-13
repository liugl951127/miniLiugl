package com.minimax.ai.marketplace.template;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * License 模板 REST API (V3.5.2 自研)
 */
@Tag(name = "模型市场 License 模板")
@RestController
@RequestMapping("/api/v1/ai/marketplace/license-template")
@RequiredArgsConstructor
public class LicenseTemplateController {

    private final LicenseTemplateService service;

    @Operation(summary = "初始化预置模板")
    @PostMapping("/seed")
    public Result<Void> seed() {
        service.seedDefaults();
        return Result.ok();
    }

    @Operation(summary = "创建模板")
    @PostMapping
    public Result<LicenseTemplate> create(@RequestBody LicenseTemplateService.LicenseTemplateDraft draft) {
        return Result.ok(service.create(draft));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<LicenseTemplate> update(@PathVariable Long id,
                                          @RequestBody LicenseTemplateService.LicenseTemplateDraft draft) {
        return Result.ok(service.update(id, draft));
    }

    @Operation(summary = "下架模板")
    @PostMapping("/{id}/deactivate")
    public Result<Void> deactivate(@PathVariable Long id) {
        boolean ok = service.deactivate(id);
        return ok ? Result.ok() : Result.fail(404, "模板不存在");
    }

    @Operation(summary = "克隆模板")
    @PostMapping("/{id}/clone")
    public Result<LicenseTemplate> clone(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        return Result.ok(service.clone(id, body.get("newKey")));
    }

    @Operation(summary = "查模板 (ID)")
    @GetMapping("/{id}")
    public Result<LicenseTemplate> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @Operation(summary = "查模板 (Key)")
    @GetMapping("/by-key/{key}")
    public Result<LicenseTemplate> getByKey(@PathVariable String key) {
        return Result.ok(service.getByKey(key));
    }

    @Operation(summary = "按类型列模板")
    @GetMapping("/by-type/{type}")
    public Result<List<LicenseTemplate>> listByType(@PathVariable String type) {
        return Result.ok(service.listByType(type));
    }

    @Operation(summary = "列公开模板")
    @GetMapping("/public")
    public Result<List<LicenseTemplate>> listPublic() {
        return Result.ok(service.listPublic());
    }

    @Operation(summary = "从模板签发 license")
    @PostMapping("/issue")
    public Result<LicenseTemplateService.IssuedLicense> issue(@RequestBody Map<String, Object> body) {
        String templateKey = (String) body.get("templateKey");
        Long userId = body.get("userId") == null ? null : ((Number) body.get("userId")).longValue();
        return Result.ok(service.issue(templateKey, userId));
    }

    @Operation(summary = "模板对比")
    @PostMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) body.get("keys");
        return Result.ok(service.compare(keys));
    }

    @Operation(summary = "模板统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(service.stats());
    }
}
