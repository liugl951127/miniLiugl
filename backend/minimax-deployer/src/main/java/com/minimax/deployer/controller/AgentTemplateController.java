package com.minimax.deployer.controller;

import com.minimax.common.result.Result;
import com.minimax.deployer.entity.AgentTemplate;
import com.minimax.deployer.service.AgentTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体模板控制器 (V2.0)
 *
 * 提供:
 *  - GET /api/v1/forge/templates               - 全部模板
 *  - GET /api/v1/forge/templates?industry=xxx  - 按行业
 *  - GET /api/v1/forge/templates/{id}          - 详情
 *  - GET /api/v1/forge/templates/code/{code}   - 按 code
 */
@Tag(name = "Agent Forge - 模板库")
@RestController
@RequestMapping("/api/v1/forge/templates")
@RequiredArgsConstructor
@Slf4j
public class AgentTemplateController {

    private final AgentTemplateService templateService;

    @GetMapping
    @Operation(summary = "全部已发布模板")
    public Result<List<AgentTemplate>> list(@RequestParam(required = false) String industry) {
        if (industry != null && !industry.isEmpty()) {
            return Result.ok(templateService.listByIndustry(industry));
        }
        return Result.ok(templateService.listPublished());
    }

    @GetMapping("/{id}")
    @Operation(summary = "模板详情")
    public Result<AgentTemplate> detail(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "按 code 查询")
    public Result<AgentTemplate> byCode(@PathVariable String code) {
        return Result.ok(templateService.getByCode(code));
    }
}
