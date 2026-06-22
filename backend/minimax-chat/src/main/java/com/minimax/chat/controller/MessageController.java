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
}
