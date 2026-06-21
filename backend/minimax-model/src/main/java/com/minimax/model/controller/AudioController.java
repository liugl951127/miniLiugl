package com.minimax.model.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 语音能力 (V4.1).
 *
 *  - ASR 语音转文字 (Whisper / Paraformer)
 *  - TTS 文字转语音 (Edge-TTS 模拟 + SiliconFlow CosyVoice)
 *
 * 端点:
 *   GET  /api/v1/audio/asr/models       列 ASR 模型
 *   POST /api/v1/audio/asr/transcribe   上传音频, 返回文字 + 时间戳
 *
 *   GET  /api/v1/audio/tts/voices       列 TTS 音色
 *   POST /api/v1/audio/tts/synthesize   文字→音频 (返回 base64 mp3)
 *
 * @since 2026-06
 */
@Slf4j
@Tag(name = "模型管理")
@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    @Value("${minimax.audio.mock-mode:true}")
    private boolean mockMode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper json = new ObjectMapper();

    // ============ ASR ============

    @Operation(summary = "列出可用 ASR 模型")
    @GetMapping("/asr/models")
    public Result<List<Map<String, Object>>> asrModels() {
        return Result.ok(List.of(
                Map.of("code", "whisper-large-v3", "displayName", "Whisper Large V3 (硅基流动)",
                        "provider", "siliconflow", "languages", "99+", "enabled", true),
                Map.of("code", "FunAudioLLM/SenseVoiceSmall", "displayName", "SenseVoice Small (阿里)",
                        "provider", "dashscope", "languages", "中英日韩", "enabled", true),
                Map.of("code", "mock", "displayName", "Mock (沙箱演示, 返回固定文字)",
                        "provider", "mock", "languages", "中文", "enabled", true)
        ));
    }

    @Operation(summary = "上传音频进行语音识别（ASR）")
    @PostMapping(value = "/asr/transcribe")
    public Result<Map<String, Object>> transcribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", defaultValue = "mock") String model,
            @RequestParam(value = "language", defaultValue = "zh") String language) {
        long t0 = System.currentTimeMillis();
        try {
            byte[] data = file.getBytes();
            long sizeKb = data.length / 1024;
            Map<String, Object> out;

            if ("mock".equals(model) || mockMode) {
                // mock: 返回固定演示文字
                out = new LinkedHashMap<>();
                out.put("text", "你好, 我是 MiniMax 智能助手. 这是 Mock 模式下的 ASR 转写结果. "
                        + "实际部署时配 OpenAI key 或硅基流动 key 即可调用真实 Whisper 模型.");
                out.put("language", language);
                out.put("durationMs", sizeKb * 50);  // 估算
                out.put("segments", List.of(
                        Map.of("start", 0, "end", 1500, "text", "你好, 我是 MiniMax 智能助手."),
                        Map.of("start", 1500, "end", 3500, "text", "这是 Mock 模式下的 ASR 转写结果."),
                        Map.of("start", 3500, "end", 5500, "text", "实际部署时配 OpenAI key 或硅基流动 key 即可调用真实 Whisper 模型.")
                ));
                out.put("sizeKb", sizeKb);
                out.put("latencyMs", System.currentTimeMillis() - t0);
                out.put("mock", true);
                out.put("model", "mock");
                return Result.ok(out);
            }

            // 真实调用 (SiliconFlow Whisper)
            String boundary = "----Minimax" + System.currentTimeMillis();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.wav";

            // multipart/form-data 构造 (简化)
            StringBuilder body = new StringBuilder();
            body.append("--").append(boundary).append("\r\n");
            body.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n").append(model).append("\r\n");
            body.append("--").append(boundary).append("\r\n");
            body.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
            body.append("Content-Type: audio/wav\r\n\r\n");

            byte[] prefix = body.toString().getBytes();
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();
            byte[] full = new byte[prefix.length + data.length + suffix.length];
            System.arraycopy(prefix, 0, full, 0, prefix.length);
            System.arraycopy(data, 0, full, prefix.length, data.length);
            System.arraycopy(suffix, 0, full, prefix.length + data.length, suffix.length);

            String apiKey = System.getenv("SILICONFLOW_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new RuntimeException("需要 SILICONFLOW_API_KEY 环境变量");
            }
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.siliconflow.cn/v1/audio/transcriptions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(full))
                    .build();
            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " " + resp.body());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body1 = json.readValue(resp.body(), Map.class);
            out = new LinkedHashMap<>();
            out.put("text", body1.getOrDefault("text", ""));
            out.put("language", body1.getOrDefault("language", language));
            out.put("sizeKb", sizeKb);
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            out.put("model", model);
            return Result.ok(out);
        } catch (Exception e) {
            log.warn("ASR 失败: {}", e.getMessage());
            return Result.fail(500, "ASR 失败: " + e.getMessage());
        }
    }

    // ============ TTS ============

    @Operation(summary = "列出可用 TTS 音色")
    @GetMapping("/tts/voices")
    public Result<List<Map<String, Object>>> ttsVoices() {
        return Result.ok(List.of(
                Map.of("code", "zh-CN-XiaoxiaoNeural", "name", "晓晓 (温柔女声)", "language", "zh-CN", "gender", "Female"),
                Map.of("code", "zh-CN-YunxiNeural", "name", "云希 (青年男声)", "language", "zh-CN", "gender", "Male"),
                Map.of("code", "zh-CN-YunyangNeural", "name", "云扬 (新闻男声)", "language", "zh-CN", "gender", "Male"),
                Map.of("code", "en-US-JennyNeural", "name", "Jenny (US Female)", "language", "en-US", "gender", "Female"),
                Map.of("code", "mock", "name", "Mock (沙箱演示)", "language", "zh", "gender", "Neutral")
        ));
    }

    @Operation(summary = "文字转语音合成（TTS）")
    @PostMapping("/tts/synthesize")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> synthesize(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        String voice = (String) body.getOrDefault("voice", "zh-CN-XiaoxiaoNeural");
        Double speed = body.get("speed") != null ? ((Number) body.get("speed")).doubleValue() : 1.0;

        if (text.isBlank()) {
            return Result.fail(400, "text 不能为空");
        }

        long t0 = System.currentTimeMillis();
        try {
            Map<String, Object> out = new LinkedHashMap<>();
            if ("mock".equals(voice) || mockMode) {
                // mock: 生成极简 WAV (1秒静音) + 文本描述
                byte[] wav = generateMockWav(text, voice);
                String b64 = Base64.getEncoder().encodeToString(wav);
                out.put("audio", "data:audio/wav;base64," + b64);
                out.put("format", "wav");
                out.put("voice", voice);
                out.put("text", text);
                out.put("speed", speed);
                out.put("sizeBytes", wav.length);
                out.put("durationMs", 1000);
                out.put("latencyMs", System.currentTimeMillis() - t0);
                out.put("mock", true);
                out.put("note", "Mock 模式, 返回 1 秒静音 WAV. 真实部署调用 Edge-TTS 或 CosyVoice");
                return Result.ok(out);
            }

            // 真实 Edge-TTS: 用 Microsoft Edge 的免费 TTS (无需 key, 但前端没法直接调 CORS)
            // 这里用硅基流动 CosyVoice
            String apiKey = System.getenv("SILICONFLOW_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new RuntimeException("需要 SILICONFLOW_API_KEY");
            }
            Map<String, Object> req = Map.of(
                    "model", "FunAudioLLM/CosyVoice2-0.5B",
                    "input", Map.of("text", text, "voice", voice.replace("zh-CN-", "").replace("Neural", "")),
                    "voice", voice,
                    "response_format", "mp3"
            );
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.siliconflow.cn/v1/audio/speech"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(req)))
                    .build();
            HttpResponse<byte[]> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            byte[] audio = resp.body();
            String b64 = Base64.getEncoder().encodeToString(audio);
            out.put("audio", "data:audio/mpeg;base64," + b64);
            out.put("format", "mp3");
            out.put("voice", voice);
            out.put("text", text);
            out.put("sizeBytes", audio.length);
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            return Result.ok(out);
        } catch (Exception e) {
            log.warn("TTS 失败: {}", e.getMessage());
            return Result.fail(500, "TTS 失败: " + e.getMessage());
        }
    }

    /**
     * 生成 1 秒静音 WAV (Mock 用).
     */
    private byte[] generateMockWav(String text, String voice) {
        int sampleRate = 16000;
        int durationMs = 1000;
        int numSamples = sampleRate * durationMs / 1000;
        int byteRate = sampleRate * 2; // 16-bit mono

        int dataSize = numSamples * 2;
        int totalSize = 36 + dataSize;

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            // RIFF header
            baos.write("RIFF".getBytes());
            writeIntLE(baos, totalSize);
            baos.write("WAVE".getBytes());
            // fmt chunk
            baos.write("fmt ".getBytes());
            writeIntLE(baos, 16);
            writeShortLE(baos, (short) 1);  // PCM
            writeShortLE(baos, (short) 1);  // mono
            writeIntLE(baos, sampleRate);
            writeIntLE(baos, byteRate);
            writeShortLE(baos, (short) 2);  // block align
            writeShortLE(baos, (short) 16); // bits per sample
            // data chunk
            baos.write("data".getBytes());
            writeIntLE(baos, dataSize);
            // 静音数据 (zeros)
            byte[] silence = new byte[dataSize];
            baos.write(silence);
        } catch (Exception e) {
            // 不会发生
        }
        return baos.toByteArray();
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
}
