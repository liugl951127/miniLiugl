package com.minimax.ai.controller;

import com.minimax.ai.generation.StreamingVideoGen;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 视频流式生成 API (V2.7.6 - SSE)
 *
 * 端点:
 *   GET  /api/ai/video/stream        SSE 实时流 (SSE 协议)
 *   GET  /api/ai/video/stream/list   列出所有任务
 *   GET  /api/ai/video/stream/{id}   任务状态
 *   POST /api/ai/video/stream/cancel/{id} 取消
 *   POST /api/ai/video/stream/demo  演示 (用 query string 也行, 这里用 POST 防缓存)
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/video/stream")
@RequiredArgsConstructor
public class VideoStreamController {

    private final StreamingVideoGen generator;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(
            @RequestParam(required = false, defaultValue = "Streaming Demo") String title,
            @RequestParam(required = false, defaultValue = "640") int width,
            @RequestParam(required = false, defaultValue = "360") int height,
            @RequestParam(required = false, defaultValue = "12") int fps,
            @RequestParam(required = false, defaultValue = "4") int duration,
            @RequestParam(required = false, defaultValue = "5") int chunkSize,
            @RequestParam(required = false, defaultValue = "80") long interval) {

        StreamingVideoGen.StreamConfig cfg = new StreamingVideoGen.StreamConfig();
        cfg.title = title;
        cfg.width = Math.min(width, 1920);
        cfg.height = Math.min(height, 1080);
        cfg.fps = Math.min(fps, 60);
        cfg.durationSeconds = Math.min(duration, 60);
        cfg.chunkSize = Math.max(1, chunkSize);
        cfg.frameIntervalMs = Math.max(20, interval);

        log.info("Stream start: {}x{} @ {}fps x {}s", cfg.width, cfg.height, cfg.fps, cfg.durationSeconds);
        return generator.stream(cfg);
    }

    @GetMapping("/list")
    public Result<Map<String, StreamingVideoGen.StreamStatus>> list() {
        return Result.ok(generator.tasks());
    }

    @GetMapping("/{id}")
    public Result<StreamingVideoGen.StreamStatus> get(@PathVariable String id) {
        StreamingVideoGen.StreamStatus s = generator.tasks().get(id);
        if (s == null) return Result.fail("任务不存在");
        return Result.ok(s);
    }

    @PostMapping("/cancel/{id}")
    public Result<Boolean> cancel(@PathVariable String id) {
        return Result.ok(generator.cancel(id));
    }
}
