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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Tag(name = "会话消息")
@Slf4j
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
        return Result.ok(Map.of("id", "stub", "sessionId", sessionId, "userId", principal.id()));
    }

    /**
     * V5.4+ SSE 流式对话 - 真接自研 AI (经 gateway lb://minimax-ai/api/v1/ai/generate)
     *
     * 之前: mock "收到你的消息: xxx" (一字一字 echo)
     * 现在: 真调 TextGenerator.generate() (含 UTF-8 / UNK / CJK / bigram / transformer 修复)
     */
    @Operation(summary = "SSE 流式对话 (V5.4+ 真接自研 AI)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long sessionId,
                                    @Valid @RequestBody AppendMessageRequest req) {
        SseEmitter emitter = new SseEmitter(120_000L);  // 2 分钟超时

        SSE_EXECUTOR.execute(() -> {
            String streamId = UUID.randomUUID().toString();
            try {
                // 1. start 事件
                SseUtil.sendBusiness(emitter, "start", Map.of(
                    "streamId", streamId,
                    "sessionId", sessionId,
                    "status", "started"
                ));

                // 2. 真接自研 AI
                String response = callLocalAi(req.getContent());

                // 3. 流式推送 (按字符, 50ms/字)
                for (int i = 0; i < response.length(); i++) {
                    char ch = response.charAt(i);
                    SseUtil.sendBusiness(emitter, "content", Map.of("content", String.valueOf(ch)));
                    Thread.sleep(50);
                }

                // 4. done 事件
                SseUtil.sendDone(emitter);
            } catch (Exception e) {
                log.warn("[Chat] stream error: {}", e.getMessage());
                SseUtil.sendError(emitter, e.getMessage());
            } finally {
                SseUtil.complete(emitter);
            }
        });

        return emitter;
    }

    /**
     * 调自研 AI (V5.4+ 真接 via gateway)
     *
     * 路径: gateway (lb://minimax-ai) → /api/v1/ai/generate → TextGenerator.generate()
     */
    private String callLocalAi(String prompt) {
        try {
            String gatewayUrl = System.getProperty("minimax.gateway.url", "http://localhost:7080");
            String url = gatewayUrl + "/api/v1/ai/generate";

            // 转义 JSON 字符串
            String safePrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            String requestBody = "{\"prompt\":\"" + safePrompt + "\",\"maxLength\":200,\"temperature\":0.8}";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.warn("[Chat] AI gateway returned {}", response.statusCode());
                return "抱歉, AI 服务暂时不可用 (HTTP " + response.statusCode() + ")";
            }

            // 解析 Result<GenerateResponse>: {"code":0,"data":{"text":"..."}}
            String body = response.body();
            Matcher m = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(body);
            if (m.find()) {
                return m.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
            return "抱歉, AI 响应解析失败";
        } catch (Exception e) {
            log.warn("[Chat] callLocalAi error: {}", e.getMessage());
            return "抱歉, 调用自研 AI 失败: " + e.getMessage();
        }
    }
}
