package com.minimax.memory;

import com.minimax.common.result.Result;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.memory.context.ContextBuilder;
import com.minimax.memory.shortterm.ShortTermMemory;
import com.minimax.memory.summary.Summarizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Memory 模块 HTTP 端点。
 *  - /memory/short-term  GET   拉短期记忆
 *  - /memory/short-term  POST  追加
 *  - /memory/short-term  DELETE 清空
 *  - /memory/context     POST  构建上下文（按 maxContext 裁剪）
 *  - /memory/summarize   POST  触发摘要压缩
 */
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final ShortTermMemory memory;
    private final ContextBuilder contextBuilder;
    private final Summarizer summarizer;

    @GetMapping("/short-term/{sessionId}")
    public Result<List<Map<String, String>>> get(@PathVariable Long sessionId,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(memory.recent(sessionId, limit));
    }

    @PostMapping("/short-term/{sessionId}")
    public Result<Void> append(@PathVariable Long sessionId,
                               @RequestBody Map<String, String> msg) {
        memory.append(sessionId, msg.get("role"), msg.get("content"));
        return Result.ok();
    }

    @DeleteMapping("/short-term/{sessionId}")
    public Result<Void> clear(@PathVariable Long sessionId) {
        memory.clear(sessionId);
        return Result.ok();
    }

    @GetMapping("/short-term/{sessionId}/size")
    public Result<Long> size(@PathVariable Long sessionId) {
        return Result.ok(memory.size(sessionId));
    }

    @PostMapping("/context/{sessionId}")
    public Result<List<Map<String, String>>> buildContext(@PathVariable Long sessionId,
                                                           @RequestBody Map<String, Object> body) {
        String systemPrompt = (String) body.getOrDefault("systemPrompt", null);
        Integer maxContext = (Integer) body.getOrDefault("maxContext", 4096);
        return Result.ok(contextBuilder.buildContext(sessionId, systemPrompt, maxContext));
    }

    @PostMapping("/summarize/{sessionId}")
    public Result<Boolean> summarize(@PathVariable Long sessionId) {
        boolean ok = summarizer.maybeSummarize(sessionId);
        return Result.ok(ok);
    }

    @GetMapping("/summary/{sessionId}")
    public Result<String> getSummary(@PathVariable Long sessionId) {
        return Result.ok(summarizer.getSummary(sessionId));
    }
}
