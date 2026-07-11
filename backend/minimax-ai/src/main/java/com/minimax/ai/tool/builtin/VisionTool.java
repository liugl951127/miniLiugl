package com.minimax.ai.tool.builtin;

import com.minimax.ai.multimodal.ImageAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 视觉分析工具 (V2.8.3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionTool extends AbstractSimpleTool {

    private final ImageAnalyzer imageAnalyzer;

    @Override
    public String getCode() { return "vision.analyze"; }

    @Override
    public String getName() { return "视觉分析"; }

    @Override
    public String getDescription() { return "图像颜色/风格/相似度"; }

    @Override
    public String getCategory() { return "vision"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String task = (String) input.getOrDefault("task", "analyze");
        Map<String, Object> result = new LinkedHashMap<>();
        switch (task) {
            case "analyze" -> {
                byte[] data = decodeImage(input);
                if (data == null) throw new IllegalArgumentException("需要 imageBase64 或 imageUrl");
                ImageAnalyzer.ImageAnalysisResult r = imageAnalyzer.analyze(data, "input");
                result.put("phash", r.phash);
                result.put("width", r.width);
                result.put("height", r.height);
                result.put("colorHistogram", r.colorHistogram);
                result.put("embedding", r.embedding);
                result.put("colorTone", r.colorTone);
                result.put("dominantColors", r.dominantColors);
                result.put("format", r.format);
                result.put("aspectRatio", r.aspectRatio);
                result.put("complexity", r.complexity);
            }
            case "compare" -> {
                String b64a = (String) input.get("imageA");
                String b64b = (String) input.get("imageB");
                if (b64a == null || b64b == null) throw new IllegalArgumentException("需要 imageA 和 imageB");
                byte[] a = Base64.getDecoder().decode(b64a);
                byte[] b = Base64.getDecoder().decode(b64b);
                ImageAnalyzer.ImageAnalysisResult ra = imageAnalyzer.analyze(a, "a");
                ImageAnalyzer.ImageAnalysisResult rb = imageAnalyzer.analyze(b, "b");
                int distance = Long.bitCount(ra.phash ^ rb.phash);
                double similarity = 1.0 - distance / 64.0;
                result.put("distance", distance);
                result.put("similarity", Math.round(similarity * 1000.0) / 1000.0);
                result.put("phashA", ra.phash);
                result.put("phashB", rb.phash);
            }
            default -> throw new IllegalArgumentException("未知 task: " + task);
        }
        return result;
    }

    private byte[] decodeImage(Map<String, Object> input) {
        String b64 = (String) input.get("imageBase64");
        return b64 != null ? Base64.getDecoder().decode(b64) : null;
    }
}
