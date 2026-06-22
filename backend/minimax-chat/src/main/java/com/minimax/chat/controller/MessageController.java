package com.minimax.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.service.ChatMessageService;
import com.minimax.chat.vo.MessageVO;
import com.minimax.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 嵌套在 session 下的消息接口：/sessions/{id}/messages
 */
@Tag(name = "聊天消息")
@Tag(name = "对话管理")
@RestController
@RequestMapping("/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatMessageService messageService;

    @Operation(summary = "获取会话消息列表")
    @GetMapping
    public Result<List<MessageVO>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable Long sessionId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return Result.ok(messageService.listBySession(principal.id(), sessionId, page, size));
    }

    @Operation(summary = "追加发送消息")
    @PostMapping
    public Result<MessageVO> append(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        return Result.ok(messageService.append(principal.id(), sessionId, req));
    }

    // V1.8: 流式会话端点 (前端 session.js 的 sendMessageStream 调用)
    // 轻量实现: 返回一个 streamId, 前端拿这个 ID 用 mockStreamResponse 降级生成内容
    // 实际生产应接 SseEmitter / Reactive Flux, 这里先保证 200 响应避免 404
    @Operation(summary = "开始流式对话 (返回 streamId)")
    @PostMapping("/stream")
    public Result<java.util.Map<String, String>> streamMessage(@AuthenticationPrincipal AuthenticatedUser principal,
                                                               @PathVariable Long sessionId,
                                                               @Valid @RequestBody AppendMessageRequest req) {
        String streamId = java.util.UUID.randomUUID().toString();
        return Result.ok(java.util.Map.of("streamId", streamId, "status", "started", "sessionId", String.valueOf(sessionId)));
    }
}
