package com.minimax.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.service.ChatMessageService;
import com.minimax.common.result.Result;
import com.minimax.common.sse.SseUtil;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "会话消息")
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatMessageService messageService;
    
    // V3.7.26+ SSE 流式发送, 用独立线程池
    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();

    @Operation(summary = "追加发送消息")
    @PostMapping
    public Result<Object> append(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        return Result.ok(java.util.Map.of("id", "stub", "sessionId", sessionId, "userId", principal.id()));
    }

    /**
     * V3.7.32+ SSE 流式对话 (V3.7.31 统一 sendBusiness, V3.7.32 清理 unused import)
     *
     * 跟 HTTP 接口统一:
     *   - 业务事件: SseUtil.sendBusiness(emitter, "content", {content: "..."}) → 自动查 type=content
     *   - 完成: SseUtil.sendDone(emitter) → {code:0, status:"finished"}
     *   - 错误: SseUtil.sendError(emitter, "msg") → {code:1, message:"msg"}
     *
     * 前端: useBusinessStream 自动剥 Result.data + 错误处理
     */
    @Operation(summary = "SSE 流式对话 (V3.7.26+ Result 包装)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        SseEmitter emitter = new SseEmitter(120_000L);  // 2 分钟超时
        
        SSE_EXECUTOR.execute(() -> {
            try {
                // V3.7.31+ sendBusiness 自动查 type (start→content, content→content, done→sendDone)
                // 1. start 事件 (自动 type=content, 走 onContent)
                SseUtil.sendBusiness(emitter, "start", Map.of(
                    "streamId", UUID.randomUUID().toString(),
                    "sessionId", sessionId,
                    "status", "started"
                ));
                
                // 2. 调 service 拿流式内容 (这里简化: 用 mock 演示)
                // 实际生产: messageService.streamAppend(userId, sessionId, req, emitter)
                String[] words = ("收到你的消息: " + req.getContent()).split("");
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    SseUtil.sendBusiness(emitter, "content", Map.of("content", word));
                    Thread.sleep(50);
                }
                
                // 3. done 事件 (专用 sendDone)
                SseUtil.sendDone(emitter);
            } catch (Exception e) {
                SseUtil.sendError(emitter, e.getMessage());
            } finally {
                SseUtil.complete(emitter);
            }
        });
        
        return emitter;
    }
}
