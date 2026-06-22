package com.minimax.chat.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1.8: 对话流控制端点
 * - /sessions/stop-stream  停止流 (前端 session.js stopMessageStream 调用)
 * - /sessions/{id}/stream-status/{streamId} 查询流状态
 *
 * 注: 完整的 SSE 流式推送由 minimax-chat + minimax-model 协作完成 (V5.x 流式架构)
 *     本 Controller 负责流生命周期管理 (start / stop / status)
 */
@Tag(name = "对话流控制")
@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class StreamController {

    /** streamId → sessionId 映射 (内存态, 进程重启失效) */
    private final Set<String> activeStreams = ConcurrentHashMap.newKeySet();

    @Operation(summary = "停止流式对话")
    @PostMapping("/stop-stream")
    public Result<Map<String, Object>> stopStream(@RequestBody Map<String, String> body) {
        String streamId = body.get("streamId");
        if (streamId == null || streamId.isEmpty()) {
            return Result.fail(400, "streamId 不能为空");
        }
        boolean removed = activeStreams.remove(streamId);
        log.info("停止流 streamId={} removed={}", streamId, removed);
        return Result.ok(Map.of(
            "streamId", streamId,
            "stopped", true,
            "existed", removed
        ));
    }

    @Operation(summary = "查询流状态")
    @GetMapping("/stream-status/{streamId}")
    public Result<Map<String, Object>> streamStatus(@PathVariable String streamId) {
        boolean active = activeStreams.contains(streamId);
        return Result.ok(Map.of(
            "streamId", streamId,
            "active", active
        ));
    }

    /** V1.8: 内部注册流 (供 StreamMessage 调用) */
    public void registerStream(String streamId) {
        activeStreams.add(streamId);
    }
}
