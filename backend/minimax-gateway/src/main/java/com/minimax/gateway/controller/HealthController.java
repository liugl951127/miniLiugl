package com.minimax.gateway.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 / 平台信息
 */
@Tag(name = "平台基础")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>(4);
        data.put("status", "UP");
        data.put("app", "minimax-platform");
        data.put("version", "1.0.0");
        data.put("day", "Day 2 - 用户体系 + JWT 鉴权");
        data.put("now", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return Result.ok(data);
    }

    @Operation(summary = "平台介绍")
    @GetMapping("/intro")
    public Result<Map<String, Object>> intro() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "MiniMax 大模型平台");
        data.put("tagline", "一站式大模型应用开发平台");
        data.put("modules", new String[]{
                "Auth - 鉴权 ✅ Day 2",
                "Chat - 会话与流式对话 (Day 3, 5)",
                "Memory - 短/长期记忆 (Day 6, 7)",
                "Model - 模型路由 (Day 4)",
                "RAG - 知识库 (Day 8)",
                "Admin - 管理后台 (Day 10)"
        });
        data.put("frontend", "http://localhost:5173");
        data.put("apiDoc", "http://localhost:8080/doc.html");
        return Result.ok(data);
    }
}
