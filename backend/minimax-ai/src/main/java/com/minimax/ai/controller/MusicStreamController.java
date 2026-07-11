package com.minimax.ai.controller;

import com.minimax.ai.generation.MusicGenerator;
import com.minimax.ai.generation.StreamingMusicGen;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 音乐流式生成 API (V2.8.1)
 *
 * 端点:
 *   GET  /api/ai/music/stream/sse     SSE 实时流
 *   GET  /api/ai/music/stream/list    任务列表
 *   GET  /api/ai/music/stream/{id}    任务详情
 *   POST /api/ai/music/stream/cancel/{id} 取消
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/music/stream")
@RequiredArgsConstructor
public class MusicStreamController {

    private final StreamingMusicGen generator;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(
            @RequestParam(required = false, defaultValue = "POP") String style,
            @RequestParam(required = false, defaultValue = "C") String key,
            @RequestParam(required = false, defaultValue = "major") String scale,
            @RequestParam(required = false, defaultValue = "120") int bpm,
            @RequestParam(required = false, defaultValue = "8") int bars,
            @RequestParam(required = false, defaultValue = "2") int chunkBars,
            @RequestParam(required = false, defaultValue = "200") long interval) {

        MusicGenerator.MusicConfig cfg = new MusicGenerator.MusicConfig();
        try { cfg.style = MusicGenerator.Style.valueOf(style.toUpperCase()); }
        catch (Exception e) { cfg.style = MusicGenerator.Style.POP; }
        cfg.key = key;
        cfg.scale = scale;
        cfg.bpm = Math.min(Math.max(bpm, 60), 240);
        cfg.bars = Math.min(Math.max(bars, 1), 64);
        cfg.beatsPerBar = 4;
        cfg.includeDrums = true;
        cfg.includeChords = true;

        StreamingMusicGen.MusicStreamConfig sc = new StreamingMusicGen.MusicStreamConfig();
        sc.config = cfg;
        sc.chunkBars = chunkBars;
        sc.barIntervalMs = interval;

        log.info("Music stream: style={} key={} bars={}", style, key, bars);
        return generator.stream(sc);
    }

    @GetMapping("/list")
    public Result<Map<String, StreamingMusicGen.StreamInfo>> list() {
        return Result.ok(generator.tasks());
    }

    @GetMapping("/{id}")
    public Result<StreamingMusicGen.StreamInfo> get(@PathVariable String id) {
        StreamingMusicGen.StreamInfo s = generator.tasks().get(id);
        if (s == null) return Result.fail("任务不存在");
        return Result.ok(s);
    }

    @PostMapping("/cancel/{id}")
    public Result<Boolean> cancel(@PathVariable String id) {
        return Result.ok(generator.cancel(id));
    }
}
