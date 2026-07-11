package com.minimax.ai.tool.builtin;

import com.minimax.ai.multimodal.AudioAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 音频分析工具 (V2.8.3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioTool extends AbstractSimpleTool {

    private final AudioAnalyzer audioAnalyzer;

    @Override
    public String getCode() { return "audio.analyze"; }

    @Override
    public String getName() { return "音频分析"; }

    @Override
    public String getDescription() { return "音量/频谱/情绪"; }

    @Override
    public String getCategory() { return "audio"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String b64 = (String) input.get("audioBase64");
        if (b64 == null) throw new IllegalArgumentException("需要 audioBase64");
        byte[] data = Base64.getDecoder().decode(b64);
        com.minimax.ai.multimodal.AudioAnalyzer.AudioAnalysisResult r = audioAnalyzer.analyze(data, "input.wav");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("format", r.format);
        result.put("sampleRate", r.sampleRate);
        result.put("channels", r.channels);
        result.put("durationMs", r.durationMs);
        result.put("rms", r.rms);
        result.put("dBFS", r.dbfs);
        result.put("zcr", r.zeroCrossingRate);
        result.put("peakAmplitude", r.peakAmplitude);
        result.put("speechRatio", r.speechRatio);
        result.put("spectrum", r.spectrum != null ? java.util.Arrays.stream(r.spectrum).boxed().collect(Collectors.toList()) : List.of());
        result.put("emotion", r.emotionTendency);
        result.put("emotionAnalysis", analyzeEmotion(r));
        return result;
    }

    private Map<String, Object> analyzeEmotion(com.minimax.ai.multimodal.AudioAnalyzer.AudioAnalysisResult r) {
        double energy = r.rms;
        double highEnergy = 0, lowEnergy = 0;
        int n = r.spectrum == null ? 0 : r.spectrum.length;
        if (n >= 4) {
            for (int i = n / 2; i < n; i++) highEnergy += r.spectrum[i];
            highEnergy /= (n / 2);
            for (int i = 0; i < n / 2; i++) lowEnergy += r.spectrum[i];
            lowEnergy /= (n / 2);
        }
        double ratio = highEnergy / Math.max(lowEnergy, 0.001);
        String label;
        if (energy < 0.05) label = "QUIET";
        else if (ratio > 1.5) label = "EXCITED";
        else if (ratio < 0.7) label = "CALM";
        else label = "NEUTRAL";
        Map<String, Object> r2 = new LinkedHashMap<>();
        r2.put("label", label);
        r2.put("energy", Math.round(energy * 1000.0) / 1000.0);
        r2.put("highLowRatio", Math.round(ratio * 1000.0) / 1000.0);
        return r2;
    }
}
