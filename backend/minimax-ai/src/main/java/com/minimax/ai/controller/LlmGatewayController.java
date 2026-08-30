package com.minimax.ai.controller;

import com.minimax.ai.llm.LlmGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LLM Gateway HTTP 控制器 (V9.0)
 *
 * 任何微服务都可以调 /api/v1/ai/llm/chat, 拿到 cloud→local 兜底结果
 * 不用自己实现重试/降级
 */
@Tag(name = "AI-LLM网关")
@RestController
@RequestMapping("/api/v1/ai/llm")
@RequiredArgsConstructor
public class LlmGatewayController {

    private final LlmGatewayService gateway;

    @PostMapping("/chat")
    @Operation(summary = "统一 chat 接口 (cloud→local 兜底)")
    public LlmGatewayService.ChatResult chat(@RequestBody ChatRequest req) {
        return gateway.chat(req.messages);
    }

    @GetMapping("/status")
    @Operation(summary = "健康检查 — 看本地 Qwen 是否就绪")
    public Map<String, Object> status() {
        return Map.of(
            "localReady", gateway.isLocalReady(),
            "fallbackEnabled", gateway.isFallbackEnabled()
        );
    }

    public static class ChatRequest {
        public List<Map<String, String>> messages;
    }
}
