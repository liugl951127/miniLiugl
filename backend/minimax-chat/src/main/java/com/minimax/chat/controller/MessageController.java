package com.minimax.chat.controller;

import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
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
@RestController
@RequestMapping("/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatMessageService messageService;

    @GetMapping
    public Result<List<MessageVO>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable Long sessionId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return Result.ok(messageService.listBySession(principal.id(), sessionId, page, size));
    }

    @PostMapping
    public Result<MessageVO> append(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        return Result.ok(messageService.append(principal.id(), sessionId, req));
    }
}
