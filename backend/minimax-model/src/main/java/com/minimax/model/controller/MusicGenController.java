package com.minimax.model.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 音乐生成 (V6.8+)
 *
 * 支持:
 *   1. SiliconFlow (FunAudioLLM/CosyVoice2-0.5B 语音/音乐)
 *   2. Mock - 返回静音 WAV + 文本描述
 *
 * 端点:
 *   GET  /api/v1/music/models    列出可用模型
 *   POST /api/v1/music/generate  文本生成音乐 (prompt / lyrics → audio)
 *
 * @since 2026-08
 */
@Slf4j
@Tag(name = "音乐生成")
@RestController
@RequestMapping("/api/v1/music")
public class MusicGenController {

    @Value("${minimax.music.mock-mode:false}")
    private boolean mockMode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Operation(summary = "列出可用音乐生成模型")
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> models() {
        List<Map<String, Object>> ms = new ArrayList<>();
        ms.add(Map.of(
                "code", "FunAudioLLM/CosyVoice2-0.5B",
                "displayName", "CosyVoice 2 (SiliconFlow)",
                "provider", "siliconflow",
                "supportsLyrics", true,
                "maxDuration", "60s",
                "pricePerSec", 0,
                "enabled", true
        ));
        ms.add(Map.of(
                "code", "mock",
                "displayName", "Mock (沙箱演示, 返回静音 WAV)",
                "provider", "mock",
                "supportsLyrics", true,
                "maxDuration", "10s",
                "pricePerSec", 0,
                "enabled", true
        ));
        return Result.ok(ms);
    }

    @Operation(summary = "文本生成音乐 (prompt + 可选歌词)")
    @PostMapping("/generate")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String prompt = (String) body.getOrDefault("prompt", "");
        String lyrics = (String) body.getOrDefault("lyrics", "");
        String model = (String) body.getOrDefault("model", "mock");

        if (prompt.isBlank() && lyrics.isBlank()) {
            return Result.fail(400, "prompt 或 lyrics 不能同时为空");
        }

        long t0 = System.currentTimeMillis();
        try {
            if ("mock".equals(model) || mockMode) {
                return Result.ok(mockMusic(prompt, lyrics, t0));
            }

            Map<String, Object> out = realMusicGen(model, prompt, lyrics, t0);
            return Result.ok(out);
        } catch (Exception e) {
            log.warn("music generate 失败: {}", e.getMessage());
            return Result.fail(500, "生成失败: " + e.getMessage());
        }
    }

    // ====================== Mock ======================

    private Map<String, Object> mockMusic(String prompt, String lyrics, long t0) {
        int sampleRate = 16000;
        int durationMs = 3000;
        int numSamples = sampleRate * durationMs / 1000;
        int byteRate = sampleRate * 2;
        int dataSize = numSamples * 2;
        int totalSize = 36 + dataSize;

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            baos.write("RIFF".getBytes());
            writeIntLE(baos, totalSize);
            baos.write("WAVE".getBytes());
            baos.write("fmt ".getBytes());
            writeIntLE(baos, 16);
            writeShortLE(baos, (short) 1);
            writeShortLE(baos, (short) 1);
            writeIntLE(baos, sampleRate);
            writeIntLE(baos, byteRate);
            writeShortLE(baos, (short) 2);
            writeShortLE(baos, (short) 16);
            baos.write("data".getBytes());
            writeIntLE(baos, dataSize);
            baos.write(new byte[dataSize]); // silence
        } catch (Exception e) { /* never */ }

        byte[] wav = baos.toByteArray();
        String b64 = Base64.getEncoder().encodeToString(wav);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("audioUrl", "data:audio/wav;base64," + b64);
        out.put("prompt", prompt);
        out.put("lyrics", lyrics);
        out.put("model", "mock");
        out.put("format", "wav");
        out.put("durationMs", durationMs);
        out.put("sizeBytes", wav.length);
        out.put("latencyMs", System.currentTimeMillis() - t0);
        out.put("mock", true);
        out.put("provider", "mock");
        out.put("note", "Mock 模式，请配置 SILICONFLOW_API_KEY 使用真实模型");
        return out;
    }

    private void writeIntLE(java.io.OutputStream o, int v) throws java.io.IOException {
        o.write(v & 0xFF);
        o.write((v >>> 8) & 0xFF);
        o.write((v >>> 16) & 0xFF);
        o.write((v >>> 24) & 0xFF);
    }

    private void writeShortLE(java.io.OutputStream o, short v) throws java.io.IOException {
        o.write(v & 0xFF);
        o.write((v >>> 8) & 0xFF);
    }

    // ====================== SiliconFlow ======================

    @SuppressWarnings("unchecked")
    private Map<String, Object> realMusicGen(String model, String prompt, String lyrics, long t0) {
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("需要 SILICONFLOW_API_KEY 环境变量，或用 model=mock");
        }
        String endpoint = "https://api.siliconflow.cn/v1/audio/speech";

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("input", Map.of(
                "text", lyrics.isBlank() ? prompt : "【歌词】" + lyrics + "\n\n【描述】" + prompt,
                "voice", "copilot"
        ));
        req.put("response_format", "mp3");

        try {
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req)))
                    .build();
            HttpResponse<byte[]> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            byte[] audio = resp.body();
            String b64 = Base64.getEncoder().encodeToString(audio);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("audioUrl", "data:audio/mpeg;base64," + b64);
            out.put("prompt", prompt);
            out.put("lyrics", lyrics);
            out.put("model", model);
            out.put("format", "mp3");
            out.put("sizeBytes", audio.length);
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            out.put("provider", "siliconflow");
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
