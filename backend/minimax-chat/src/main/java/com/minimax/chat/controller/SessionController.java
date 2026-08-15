package com.minimax.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimax.chat.dto.CreateSessionRequest;
import com.minimax.chat.dto.UpdateSessionRequest;
import com.minimax.chat.entity.ChatMessage;
import com.minimax.chat.entity.ChatSession;
import com.minimax.chat.mapper.ChatMessageMapper;
import com.minimax.chat.mapper.ChatSessionMapper;
import com.minimax.chat.service.ChatSessionService;
import com.minimax.chat.vo.SessionVO;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "聊天会话")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService sessionService;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;

    @Operation(summary = "获取会话列表")
    @GetMapping
    public Result<List<SessionVO>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @RequestParam(required = false) Integer status) {
        return Result.ok(sessionService.listByUser(principal.id(), status));
    }

    @Operation(summary = "创建新会话")
    @PostMapping
    public Result<SessionVO> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @Valid @RequestBody CreateSessionRequest req) {
        return Result.ok(sessionService.create(principal.id(), req));
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/{id}")
    public Result<SessionVO> detail(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long id) {
        return Result.ok(sessionService.detail(id, principal.id()));
    }

    @Operation(summary = "更新会话信息")
    @PutMapping("/{id}")
    public Result<SessionVO> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody UpdateSessionRequest req) {
        return Result.ok(sessionService.update(id, principal.id(), req));
    }

    @Operation(summary = "归档删除会话")
    @DeleteMapping("/{id}")
    public Result<Void> archive(@AuthenticationPrincipal AuthenticatedUser principal,
                                @PathVariable Long id) {
        sessionService.archive(id, principal.id());
        return Result.ok();
    }

    // ============ 消息收发 ============

    @Operation(summary = "发送消息（持久化）")
    @PostMapping("/{id}/messages")
    public Result<ChatMessage> sendMessage(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        // 校验权限
        sessionService.requireOwned(id, principal.id());

        String role = (String) body.getOrDefault("role", "user");
        String content = (String) body.getOrDefault("content", "");
        String model = (String) body.getOrDefault("model", "");

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(id);
        msg.setUserId(principal.id());
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        // 更新 session 的 lastMessageAt 和 messageCount
        ChatSession session = sessionMapper.selectById(id);
        if (session != null) {
            session.setLastMessageAt(LocalDateTime.now());
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
            if (model != null && !model.isBlank()) {
                session.setModel(model);
            }
            sessionMapper.updateById(session);
        }

        log.info("[Chat] 消息发送: session={} role={} user={}", id, role, principal.id());
        return Result.ok(msg);
    }

    @Operation(summary = "获取会话消息列表")
    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> getMessages(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long id,
                                                 @RequestParam(defaultValue = "50") int limit) {
        // V6.8.2: 边界校验，防止过大 limit
        limit = Math.max(1, Math.min(limit, 500));
        sessionService.requireOwned(id, principal.id());

        QueryWrapper<ChatMessage> qw = new QueryWrapper<>();
        qw.eq("session_id", id).orderByAsc("id").last("LIMIT " + limit);
        return Result.ok(messageMapper.selectList(qw));
    }

}
