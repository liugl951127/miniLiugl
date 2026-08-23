package com.minimax.ai.controller;

import com.minimax.ai.multimodal.audio.OnnxSileroVadService;
import com.minimax.ai.multimodal.video.OnnxVideoAnalyzerService;
import com.minimax.ai.embedding.onnx.OnnxBgeEmbeddingService;
import com.minimax.ai.llm.onnx.OnnxQwenChatService;
import com.minimax.ai.multimodal.audio.OnnxWhisperService;
import com.minimax.ai.multimodal.audio.WavReader;
import com.minimax.ai.multimodal.onnx.OnnxClipService;
import com.minimax.ai.multimodal.onnx.OnnxObjectDetectorService;
import com.minimax.ai.multimodal.onnx.OnnxResNet50Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * ONNX 多模态 API (V7.1)
 *
 * <h3>端点</h3>
 * <ul>
 *   <li><code>GET  /api/v1/multimodal/status</code> — 3 个模型的就绪状态</li>
 *   <li><code>POST /api/v1/multimodal/classify</code> — 图片分类 (multipart file, topK)</li>
 *   <li><code>POST /api/v1/multimodal/detect</code> — 目标检测 (YOLOv8)</li>
 *   <li><code>POST /api/v1/multimodal/encode-image</code> — 图片 embedding</li>
 *   <li><code>POST /api/v1/multimodal/encode-text</code> — 文本 embedding</li>
 *   <li><code>POST /api/v1/multimodal/text-image-similarity</code> — 文图相似度</li>
 * </ul>
 *
 * <p>鉴权: 通过 @AuthenticationPrincipal 走 Spring Security, gateway 注入 X-User-Id</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/multimodal")
@RequiredArgsConstructor
public class OnnxMultimodalController {

    private final OnnxResNet50Service resnet;
    private final OnnxClipService clip;
    private final OnnxObjectDetectorService detector;
    private final OnnxWhisperService whisper;
    private final OnnxSileroVadService vad;
    private final OnnxVideoAnalyzerService video;
    private final OnnxBgeEmbeddingService bge;
    private final OnnxQwenChatService qwen;

    // ─── 1. 状态 ──────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("resnet50", Map.of(
            "enabled", resnet.isEnabled(),
            "ready",   resnet.isReady(),
            "path",    resnet.getModelPath()
        ));
        m.put("clip", Map.of(
            "enabled", clip.isEnabled(),
            "ready",   clip.isReady()
        ));
        m.put("yolo", Map.of(
            "enabled", detector.isEnabled(),
            "ready",   detector.isReady(),
            "path",    detector.getModelPath()
        ));
        m.put("whisper", Map.of(
            "enabled", true,
            "ready",   whisper.isReady(),
            "path",    whisper.getModelPath()
        ));
        m.put("vad", Map.of(
            "enabled", true,
            "ready",   vad.isReady(),
            "path",    vad.getModelPath(),
            "threshold", vad.getThreshold()
        ));
        m.put("video", Map.of(
            "enabled", true,
            "available", video.isAvailable(),
            "requiresFfmpeg", true
        ));
        m.put("bge", Map.of(
            "enabled", true,
            "ready", bge.isReady(),
            "path", bge.getModelPath(),
            "dim", bge.isReady() ? bge.getEmbeddingDim() : 0
        ));
        m.put("qwen", Map.of(
            "enabled", true,
            "ready", qwen.isReady(),
            "path", qwen.getModelPath(),
            "vocab", qwen.getVocabSize()
        ));
        m.put("version", "V7.4");
        return ResponseEntity.ok(Map.of("code", 0, "data", m));
    }

    // ─── 2. 图片分类 ─────────────────────────────────────

    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classify(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        try {
            BufferedImage image = readImage(file);
            if (image == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "图片解析失败"));
            }
            if (!resnet.isReady()) {
                return ResponseEntity.ok(Map.of(
                    "code", 1001,
                    "message", "ResNet50 模型未就绪, 请先执行 scripts/download-models.sh resnet",
                    "data", Collections.emptyList()
                ));
            }
            List<OnnxResNet50Service.ClassificationResult> results = resnet.classify(image, topK);
            List<Map<String, Object>> data = new ArrayList<>(results.size());
            for (OnnxResNet50Service.ClassificationResult r : results) data.add(r.toMap());
            return ResponseEntity.ok(Map.of("code", 0, "data", data));
        } catch (Exception e) {
            log.error("classify 失败", e);
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 3. 目标检测 ─────────────────────────────────────

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detect(@RequestParam("file") MultipartFile file) {
        try {
            BufferedImage image = readImage(file);
            if (image == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "图片解析失败"));
            }
            if (!detector.isReady()) {
                return ResponseEntity.ok(Map.of(
                    "code", 1001,
                    "message", "YOLOv8 模型未就绪, 请先执行 scripts/download-models.sh yolo",
                    "data", Collections.emptyList()
                ));
            }
            List<OnnxObjectDetectorService.Detection> dets = detector.detect(image);
            List<Map<String, Object>> data = new ArrayList<>(dets.size());
            for (OnnxObjectDetectorService.Detection d : dets) data.add(d.toMap());
            return ResponseEntity.ok(Map.of("code", 0, "data", data));
        } catch (Exception e) {
            log.error("detect 失败", e);
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 4. 图片 embedding ────────────────────────────────

    @PostMapping("/encode-image")
    public ResponseEntity<Map<String, Object>> encodeImage(@RequestParam("file") MultipartFile file) {
        try {
            BufferedImage image = readImage(file);
            if (image == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "图片解析失败"));
            }
            float[] vec = clip.encodeImage(image);
            return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
                "dim", vec.length,
                "vector", vec,
                "ready", clip.isReady()
            )));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 5. 文本 embedding ────────────────────────────────

    @PostMapping("/encode-text")
    public ResponseEntity<Map<String, Object>> encodeText(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        float[] vec = clip.encodeText(text);
        return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
            "text", text,
            "dim", vec.length,
            "vector", vec,
            "ready", clip.isReady()
        )));
    }

    // ─── 6. 文图相似度 ───────────────────────────────────

    @PostMapping("/text-image-similarity")
    public ResponseEntity<Map<String, Object>> similarity(
            @RequestParam("file") MultipartFile file,
            @RequestParam("text") String text) {
        try {
            BufferedImage image = readImage(file);
            if (image == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "图片解析失败"));
            }
            float score = clip.similarity(text, image);
            return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
                "text", text,
                "score", score,
                "ready", clip.isReady()
            )));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 7. 语音转文字 (Whisper-tiny ONNX) ─────────────────

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lang", defaultValue = "zh") String lang) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "音频文件为空"));
            }
            if (!whisper.isReady()) {
                return ResponseEntity.ok(Map.of("code", 1001,
                    "message", "Whisper 模型未就绪, 请先执行 scripts/download-models.sh whisper",
                    "data", Map.of()));
            }
            float[] pcm = WavReader.readAsMonoFloat16k(file.getBytes());
            if (pcm.length == 0) {
                return ResponseEntity.ok(Map.of("code", 400,
                    "message", "音频解析失败 (仅支持 WAV/PCM, MP3 需 ffmpeg 预转码)"));
            }
            OnnxWhisperService.TranscribeResult result = whisper.transcribe(pcm, lang);
            return ResponseEntity.ok(Map.of("code", result.isSuccess() ? 0 : 500, "data", result.toMap()));
        } catch (Exception e) {
            log.error("transcribe 失败", e);
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 8. 语音活动检测 (Silero VAD) ─────────────────────

    @PostMapping("/vad")
    public ResponseEntity<Map<String, Object>> vadDetect(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "音频文件为空"));
            }
            if (!vad.isReady()) {
                return ResponseEntity.ok(Map.of("code", 1001,
                    "message", "Silero VAD 模型未就绪",
                    "data", Collections.emptyList()));
            }
            float[] pcm = WavReader.readAsMonoFloat16k(file.getBytes());
            if (pcm.length == 0) {
                return ResponseEntity.ok(Map.of("code", 400, "message", "音频解析失败"));
            }
            List<OnnxSileroVadService.SpeechSegment> segs = vad.detectSegments(pcm);
            List<Map<String, Object>> data = new ArrayList<>(segs.size());
            for (OnnxSileroVadService.SpeechSegment s : segs) data.add(s.toMap());
            return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
                "segments", data,
                "totalDuration", (float) pcm.length / 16000,
                "speechRatio", segs.isEmpty() ? 0f :
                    segs.stream().map(OnnxSileroVadService.SpeechSegment::duration)
                        .reduce(0f, Float::sum) / ((float) pcm.length / 16000)
            )));
        } catch (Exception e) {
            log.error("vad 失败", e);
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 9. 视频智能分析 (复用 ResNet50 + Whisper) ──────────

    @PostMapping("/analyze-video")
    public ResponseEntity<Map<String, Object>> analyzeVideo(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "视频文件为空"));
            }
            if (!video.isAvailable()) {
                return ResponseEntity.ok(Map.of("code", 1001,
                    "message", "视频分析需要: ffmpeg + (ResNet50 或 Whisper) 至少一个就绪",
                    "data", Map.of()));
            }
            OnnxVideoAnalyzerService.VideoAnalysisResult result = video.analyze(file.getBytes());
            return ResponseEntity.ok(Map.of(
                "code", result.success() ? 0 : 500,
                "data", result.toMap(),
                "message", result.error()
            ));
        } catch (Exception e) {
            log.error("analyze-video 失败", e);
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 10. 文本 Embedding (BGE-zh) ───────────────────────

    @PostMapping("/embed-text")
    public ResponseEntity<Map<String, Object>> embedText(@RequestBody Map<String, Object> body) {
        try {
            Object input = body.get("texts");
            String[] texts;
            if (input instanceof List) {
                List<?> list = (List<?>) input;
                texts = list.stream().map(Object::toString).toArray(String[]::new);
            } else if (input instanceof String) {
                texts = new String[]{(String) input};
            } else {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "texts 字段缺失或格式错"));
            }
            if (!bge.isReady()) {
                return ResponseEntity.ok(Map.of("code", 1001,
                    "message", "BGE 模型未就绪, 请先执行 scripts/download-models.sh bge",
                    "data", Map.of()));
            }
            float[][] embs = bge.encodeBatch(texts);
            List<Map<String, Object>> data = new ArrayList<>(texts.length);
            for (int i = 0; i < embs.length; i++) {
                data.add(Map.of("index", i, "text", texts[i], "dim", embs[i].length, "vector", embs[i]));
            }
            return ResponseEntity.ok(Map.of("code", 0, "data", data));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── 11. Qwen2.5 对话 ────────────────────────────────

    @PostMapping("/chat-qwen")
    public ResponseEntity<Map<String, Object>> chatQwen(@RequestBody Map<String, Object> body) {
        try {
            String prompt = (String) body.getOrDefault("prompt", "");
            String system = (String) body.getOrDefault("system", null);
            Integer maxTokens = (Integer) body.getOrDefault("maxTokens", null);
            if (!qwen.isReady()) {
                return ResponseEntity.ok(Map.of("code", 1001,
                    "message", "Qwen2.5 模型未就绪, 请先执行 scripts/download-models.sh qwen",
                    "data", Map.of()));
            }
            OnnxQwenChatService.ChatResult result = maxTokens != null
                ? qwen.chat(prompt, system, maxTokens)
                : qwen.chat(prompt, system);
            return ResponseEntity.ok(Map.of(
                "code", result.isSuccess() ? 0 : 500,
                "data", result.toMap(),
                "message", result.error()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    // ─── util ────────────────────────────────────────────

    private BufferedImage readImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(file.getBytes())) {
            return ImageIO.read(bis);
        } catch (Exception e) {
            log.warn("图片解析失败: {}", e.getMessage());
            return null;
        }
    }
}
