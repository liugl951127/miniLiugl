package com.minimax.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.dto.MessageVO;
import com.minimax.chat.service.MessageService;
import com.minimax.common.result.Result;
import com.minimax.common.sse.SseResult;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "会话消息")
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    
    // V3.7.26+ SSE 流式发送, 用独立线程池
    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();

    @Operation(summary = "追加发送消息")
    @PostMapping
    public Result<MessageVO> append(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        return Result.ok(messageService.append(principal.id(), sessionId, req));
    }

    /**
     * V3.7.26+ SSE 流式对话
     * 
     * 跟 HTTP 接口统一:
     *   - 成功: SseResult.send(emitter, "content", {content: "..."}) → {code:0, data:{content:"..."}}
     *   - 错误: SseResult.sendError(emitter, "msg") → {code:1, data:{message:"msg"}}
     *   - 完成: SseResult.sendDone(emitter) → {code:0, data:{status:"finished"}}
     * 
     * 前端: useBusinessStream 自动剥 Result.data + 错误处理
     */
    @Operation(summary = "SSE 流式对话 (V3.7.26+ Result 包装)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        Long userId = principal != null ? principal.id() : null;
        SseEmitter emitter = new SseEmitter(120_000L);  // 2 分钟超时
        
        SSE_EXECUTOR.execute(() -> {
            try {
                // 1. start 事件 (V3.7.26+ 5 type 统一)
                Map<String, Object> startData = new LinkedHashMap<>();
                startData.put("streamId", UUID.randomUUID().toString());
                startData.put("sessionId", sessionId);
                startData.put("status", "started");
                SseResult.send(emitter, "start", startData);
                
                // 2. 调 service 拿流式内容 (这里简化: 用 mock 演示)
                // 实际生产: messageService.streamAppend(userId, sessionId, req, emitter)
                String[] words = ("收到你的消息: " + req.getContent()).split("");
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    SseResult.send(emitter, "content", Map.of("content", word));
                    Thread.sleep(50);
                }
                
                // 3. done 事件
                SseResult.sendDone(emitter);
            } catch (Exception e) {
                SseResult.sendError(emitter, e.getMessage());
            } finally {
                SseResult.complete(emitter);
            }
        });
        
        return emitter;
    }
}
