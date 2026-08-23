package com.minimax.ai.controller;

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
        m.put("version", "V7.1");
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
