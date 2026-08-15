package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI Audio 代理控制器 (V6.8+)
 *
 * TTS / ASR 真实化 — 代理到 minimax-model AudioController。
 * 无 key 时优雅降级。
 *
 * @since 2026-08
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/audio")
public class AiAudioRealController {

    @Autowired(required = false)
    private RestTemplate modelRestTemplate;

    private static final String MODEL_BASE = "http://minimax-model:8084";

    /**
     * TTS — 代理到 minimax-model AudioController
     */
    @PostMapping("/tts")
    public Result<Map<String, Object>> tts(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        String voice = (String) body.getOrDefault("voice", "zh-CN-XiaoxiaoNeural");
        log.info("[TTS] text={} voice={}", text, voice);

        if (text == null || text.isBlank()) {
            return Result.fail(400, "text 不能为空");
        }

        try {
            if (modelRestTemplate != null) {
                ResponseEntity<Map> resp = modelRestTemplate.postForEntity(
                        MODEL_BASE + "/api/v1/audio/tts/synthesize",
                        body, Map.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Map<String, Object> out = new LinkedHashMap<>(resp.getBody());
                    out.put("mock", false);
                    return Result.ok(out);
                }
            }
        } catch (Exception e) {
            log.warn("[TTS] 代理失败: {}", e.getMessage());
        }

        // 降级
        return Result.ok(Map.of(
                "text", text,
                "voice", voice,
                "audioUrl", null,
                "durationMs", text.length() * 80L,
                "format", "mp3",
                "mock", true,
                "note", "TTS 需要配置 SILICONFLOW_API_KEY"
        ));
    }

    /**
     * ASR — 代理到 minimax-model AudioController
     */
    @PostMapping("/asr")
    public Result<Map<String, Object>> asr(
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "audioUrl", required = false) String audioUrl,
            @RequestParam(value = "model", defaultValue = "mock") String model,
            @RequestParam(value = "language", defaultValue = "zh") String language) {

        log.info("[ASR] file={} audioUrl={} model={}", file != null ? file.getOriginalFilename() : "null", audioUrl, model);

        // 如果前端传的是 URL 形式的 audioUrl（降级场景）
        if (audioUrl != null && !audioUrl.isBlank() && file == null) {
            return Result.ok(Map.of(
                    "text", "【需要配置 SILICONFLOW_API_KEY】\n\n"
                            + "请配置 SILICONFLOW_API_KEY 后重新上传音频文件进行识别。",
                    "language", language,
                    "mock", true,
                    "note", "ASR 需要 SILICONFLOW_API_KEY"
            ));
        }

        if (file == null) {
            return Result.fail(400, "请上传音频文件或提供 audioUrl");
        }

        try {
            if (modelRestTemplate != null) {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                org.springframework.util.MultiValueMap<String, Object> parts =
                        new org.springframework.util.LinkedMultiValueMap<>();
                parts.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                    @Override public String getFilename() { return file.getOriginalFilename(); }
                });
                parts.add("model", model);
                parts.add("language", language);

                HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                        new HttpEntity<>(parts, headers);
                ResponseEntity<Map> resp = modelRestTemplate.exchange(
                        MODEL_BASE + "/api/v1/audio/asr/transcribe",
                        HttpMethod.POST, entity, Map.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Map<String, Object> out = new LinkedHashMap<>(resp.getBody());
                    out.put("mock", false);
                    return Result.ok(out);
                }
            }
        } catch (Exception e) {
            log.warn("[ASR] 代理失败: {}", e.getMessage());
        }

        return Result.ok(Map.of(
                "text", "【需要配置 SILICONFLOW_API_KEY】\n\n"
                        + "请配置 SILICONFLOW_API_KEY 环境变量后重新上传音频文件进行识别。",
                "language", language,
                "mock", true,
                "note", "ASR 需要 SILICONFLOW_API_KEY"
        ));
    }
}
