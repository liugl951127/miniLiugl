package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台介绍端点 (V3.5.7 新增)
 *
 * <p>供前端 About.vue 调, 返回平台基本信息:
 * <ul>
 *   <li>app: 应用名</li>
 *   <li>version: 版本号</li>
 *   <li>day: 启动天数 (从 2022-01-01 算)</li>
 *   <li>modules: 启用的模块列表</li>
 *   <li>now: 服务器当前时间</li>
 * </ul>
 *
 * <h2>API 路径</h2>
 * <pre>{@code
 *   GET /api/ai/intro
 * }</pre>
 *
 * @author MiniMax
 * @since V3.5.7
 */
@Tag(name = "平台介绍")
@RestController
@RequestMapping("/api/ai")
public class IntroController {

    @Value("${spring.application.name:minimax-platform}")
    private String appName;

    @Value("${minimax.version:V3.5.7}")
    private String version;

    private static final LocalDate FOUNDING = LocalDate.of(2022, 1, 1);

    @Operation(summary = "获取平台介绍信息")
    @GetMapping("/intro")
    public Result<Map<String, Object>> intro() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", "MiniMax Platform");
        info.put("name", appName);
        info.put("version", version);
        info.put("day", LocalDate.now().toEpochDay() - FOUNDING.toEpochDay() + 1);
        info.put("tagline", "企业级 AI Agent 平台 · 17 微服务 · 自研 LLM 推理引擎");
        info.put("foundedYear", "2022");
        info.put("now", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        // 模块列表
        info.put("modules", new String[]{
                "auth", "ai", "chat", "memory", "model", "rag", "function",
                "multimodal", "agent", "monitor", "admin", "prompt",
                "analytics", "pipeline", "ws", "gateway"
        });
        return Result.ok(info);
    }
}
