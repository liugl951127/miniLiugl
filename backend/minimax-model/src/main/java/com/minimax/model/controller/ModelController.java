package com.minimax.model.controller;

import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.vo.ModelVO;
import com.minimax.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    /** 列出所有可用模型。 */
    @GetMapping
    public Result<List<ModelVO>> list() {
        return Result.ok(modelService.listEnabled());
    }

    /** 列出所有 provider。 */
    @GetMapping("/providers")
    public Result<List<String>> providers() {
        return Result.ok(List.of("openai", "minimax", "ollama", "zhipu", "qwen", "deepseek"));
    }

    /** 阻塞 chat 端点。 */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @Valid @RequestBody ChatRequest req) {
        return Result.ok(modelService.chat(principal.id(), req));
    }

    /**
     * 流式 chat 端点 (SSE)。
     * 用 Spring MVC StreamingResponseBody 避免 webflux 冲突。
     * 路径: POST /models/chat/stream
     * 响应: text/event-stream  (data: {...}\n\n)
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamChat(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @RequestBody ChatRequest req) {
        return output -> {
            // 同步模拟: 每 30ms 推一个字符
            try {
                com.minimax.model.vo.ChatResponse resp = modelService.chat(principal.id(), req);
                String text = resp.getContent() == null ? "" : resp.getContent();
                for (char c : text.toCharArray()) {
                    String chunk = "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + escapeJson(c+"") + "\"}}]}\n\n";
                    output.write(chunk.getBytes());
                    output.flush();
                    Thread.sleep(30);
                }
                output.write("data: [DONE]\n\n".getBytes());
                output.flush();
            } catch (Exception e) {
                output.write(("data: {\"error\":\"" + e.getMessage() + "\"}\n\n").getBytes());
            }
        };
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
