package com.minimax.chat.controller;

import com.minimax.chat.dto.AiGenerateRequest;
import com.minimax.chat.service.ChatAiService;
import com.minimax.common.result.Result;
import com.minimax.common.sdk.LlmClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.minimax.common.security.AuthenticatedUser;

/**
 * Chat AI 控制器 (V9.0) — 后端调 LLM, 显式返回 source
 *
 * 端点:
 *  POST /api/v1/sessions/{sessionId}/ai-generate
 *   body: { content, system?, history? }
 *   resp: { code, data: { content, source, model, durationMs, reason, available } }
 *
 *  source 含义:
 *   CLOUD         云端 (GPT-4o / DeepSeek) 成功
 *   LOCAL         本地 Qwen2.5-0.5B 成功 (云端未配置)
 *   LOCAL_FALLBACK 云端失败, 本地兜底
 *   UNAVAILABLE   都没成功, content=null
 */
@Tag(name = "Chat-AI")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class AiController {

    private final ChatAiService chatAiService;

    @PostMapping("/{sessionId}/ai-generate")
    @Operation(summary = "AI 生成回复 (cloud→local 兜底)")
    public Result<LlmClient.LlmResult> generate(
            @PathVariable Long sessionId,
            @RequestBody AiGenerateRequest req,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Long userId = principal != null ? principal.id() : null;
        LlmClient.LlmResult result = chatAiService.generateAiReply(sessionId, userId, req);
        // 即使 available=false 也返回 200, 前端根据 available 字段决定是否显示降级提示
        return Result.ok(result);
    }
}
