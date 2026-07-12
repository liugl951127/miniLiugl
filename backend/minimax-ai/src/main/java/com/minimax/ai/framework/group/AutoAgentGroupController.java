package com.minimax.ai.framework.group;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 一句话生成智能体群 REST API (V3.4.2)
 */
@Tag(name = "智能体群自动生成")
@RestController
@RequestMapping("/api/v1/ai/agent-group/auto")
@RequiredArgsConstructor
public class AutoAgentGroupController {

    private final AutoAgentGroupGenerator generator;

    @Operation(summary = "一句话生成群组")
    @PostMapping("/generate")
    public Result<AutoAgentGroupGenerator.GeneratedGroup> generate(@RequestBody Map<String, String> body) {
        String oneLiner = body.get("oneLiner");
        return Result.ok(generator.generate(oneLiner));
    }

    @Operation(summary = "按模板生成")
    @PostMapping("/template")
    public Result<AutoAgentGroupGenerator.GeneratedGroup> fromTemplate(@RequestBody Map<String, String> body) {
        return Result.ok(generator.generateFromTemplate(body.get("template"), body.get("description")));
    }

    @Operation(summary = "列出模板")
    @GetMapping("/templates")
    public Result<Map<String, AutoAgentGroupGenerator.GroupTemplate>> listTemplates() {
        return Result.ok(generator.listTemplates());
    }
}
