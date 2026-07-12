package com.minimax.memory;

import com.minimax.common.result.Result;
import com.minimax.memory.context.CrossSessionContextBuilder;
import com.minimax.memory.longterm.LongTermMemory;
import com.minimax.memory.longterm.LongTermMemoryService;
import com.minimax.memory.pref.UserPref;
import com.minimax.memory.pref.UserPrefService;
import com.minimax.memory.shortterm.ShortTermMemory;
import com.minimax.memory.summary.Summarizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Memory 模块 HTTP 端点 (Day 7 完整版)。
 *
 * 短期记忆 (Short-Term):
 *   GET    /memory/short-term/{sid}        拉取
 *   POST   /memory/short-term/{sid}        追加
 *   DELETE /memory/short-term/{sid}        清空
 *   GET    /memory/short-term/{sid}/size   统计
 *
 * 上下文 (Context):
 *   POST   /memory/context/{sid}           单会话 context
 *   POST   /memory/cross-context           跨会话 context (短期+长期+偏好)
 *
 * 摘要 (Summary):
 *   POST   /memory/summarize/{sid}         触发摘要 (LLM)
 *   GET    /memory/summary/{sid}           读取摘要
 *
 * 长期记忆 (Long-Term Vector):
 *   POST   /memory/long-term                存储
 *   POST   /memory/long-term/recall         召回
 *   GET    /memory/long-term/recent        列出最近
 *   DELETE /memory/long-term/{id}          删除
 *
 * 用户偏好 (User Pref):
 *   PUT    /memory/pref/{key}              设置
 *   GET    /memory/pref/{key}              读取
 *   GET    /memory/pref                    列出所有
 *   DELETE /memory/pref/{key}              删除
 */
@Tag(name = "记忆管理")
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final ShortTermMemory memory;
    private final LongTermMemoryService longTerm;
    private final UserPrefService prefs;
    private final Summarizer summarizer;
    private final CrossSessionContextBuilder crossCtx;

    // ---------- 短期记忆 ----------

    @Operation(summary = "获取短期记忆")
    @GetMapping("/short-term/{sessionId}")
    public Result<List<Map<String, String>>> get(@PathVariable Long sessionId,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(memory.recent(sessionId, limit));
    }

    @Operation(summary = "追加短期记忆")
    @PostMapping("/short-term/{sessionId}")
    public Result<Void> append(@PathVariable Long sessionId,
                               @RequestBody Map<String, String> msg) {
        memory.append(sessionId, msg.get("role"), msg.get("content"));
        return Result.ok();
    }

    @Operation(summary = "清空短期记忆")
    @DeleteMapping("/short-term/{sessionId}")
    public Result<Void> clear(@PathVariable Long sessionId) {
        memory.clear(sessionId);
        return Result.ok();
    }

    @Operation(summary = "获取短期记忆大小")
    @GetMapping("/short-term/{sessionId}/size")
    public Result<Long> size(@PathVariable Long sessionId) {
        return Result.ok(memory.size(sessionId));
    }

    // ---------- 上下文 ----------

    @Operation(summary = "构建单会话上下文")
    @PostMapping("/context/{sessionId}")
    public Result<List<Map<String, String>>> buildContext(@PathVariable Long sessionId,
                                                           @RequestBody Map<String, Object> body) {
        String sys = (String) body.getOrDefault("systemPrompt", null);
        Integer max = (Integer) body.getOrDefault("maxContext", 4096);
        return Result.ok(crossCtx.build(0L, sessionId, sys, max, 5));
    }

    @Operation(summary = "构建跨会话上下文")
    @PostMapping("/cross-context")
    public Result<List<Map<String, String>>> crossContext(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long sessionId = ((Number) body.get("sessionId")).longValue();
        String sys = (String) body.getOrDefault("systemPrompt", null);
        Integer max = (Integer) body.getOrDefault("maxContext", 4096);
        Integer topK = (Integer) body.getOrDefault("recallTopK", 5);
        return Result.ok(crossCtx.build(userId, sessionId, sys, max, topK));
    }

    // ---------- 摘要 ----------

    @Operation(summary = "触发会话摘要")
    @PostMapping("/summarize/{sessionId}")
    public Result<Boolean> summarize(@PathVariable Long sessionId) {
        return Result.ok(summarizer.maybeSummarize(sessionId));
    }

    @Operation(summary = "获取会话摘要")
    @GetMapping("/summary/{sessionId}")
    public Result<String> getSummary(@PathVariable Long sessionId) {
        return Result.ok(summarizer.getSummary(sessionId));
    }

    // ---------- 长期记忆 ----------

    @Operation(summary = "存储长期记忆")
    @PostMapping("/long-term")
    public Result<Long> storeLong(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long sessionId = body.get("sessionId") == null ? null : ((Number) body.get("sessionId")).longValue();
        String role = (String) body.getOrDefault("role", "user");
        String content = (String) body.get("content");
        String summary = (String) body.get("summary");
        String tags = (String) body.get("tags");
        Double importance = body.get("importance") == null ? null : ((Number) body.get("importance")).doubleValue();
        return Result.ok(longTerm.store(userId, sessionId, role, content, summary, tags, importance));
    }

    @Operation(summary = "召回长期记忆")
    @PostMapping("/long-term/recall")
    public Result<List<LongTermMemoryService.RecallHit>> recall(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String query = (String) body.get("query");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        return Result.ok(longTerm.recall(userId, query, topK));
    }

    @Operation(summary = "获取最近长期记忆")
    @GetMapping("/long-term/recent")
    public Result<List<LongTermMemory>> recent(@RequestParam Long userId,
                                                @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(longTerm.recent(userId, limit));
    }

    @Operation(summary = "删除长期记忆")
    @DeleteMapping("/long-term/{id}")
    public Result<Boolean> deleteLong(@PathVariable Long id, @RequestParam Long userId) {
        return Result.ok(longTerm.delete(id, userId));
    }

    // ---------- 用户偏好 ----------

    @Operation(summary = "设置用户偏好")
    @PutMapping("/pref/{key}")
    public Result<Void> setPref(@RequestParam Long userId,
                                 @PathVariable String key,
                                 @RequestBody Map<String, String> body) {
        prefs.set(userId, key, body.get("value"), body.getOrDefault("source", "manual"));
        return Result.ok();
    }

    @Operation(summary = "获取用户偏好")
    @GetMapping("/pref/{key}")
    public Result<String> getPref(@RequestParam Long userId, @PathVariable String key) {
        return Result.ok(prefs.get(userId, key));
    }

    @Operation(summary = "列出用户所有偏好")
    @GetMapping("/pref")
    public Result<List<UserPref>> listPref(@RequestParam Long userId) {
        return Result.ok(prefs.listByUser(userId));
    }

    @Operation(summary = "删除用户偏好")
    @DeleteMapping("/pref/{key}")
    public Result<Boolean> deletePref(@RequestParam Long userId, @PathVariable String key) {
        return Result.ok(prefs.delete(userId, key));
    }
}