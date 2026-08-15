package com.minimax.ai.tool.builtin;

import com.minimax.ai.generation.MusicGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 音乐生成工具 (V2.8.3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicGenTool extends AbstractSimpleTool {

    private final MusicGenerator musicGenerator;

    @Override
    public String getCode() { return "music.generate"; }

    @Override
    public String getName() { return "音乐生成"; }

    @Override
    public String getDescription() { return "MIDI 音乐 (6 风格 7 调式)"; }

    @Override
    public String getCategory() { return "music"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        MusicGenerator.MusicConfig cfg = new MusicGenerator.MusicConfig();
        String styleStr = (String) input.getOrDefault("style", "POP");
        try { cfg.style = MusicGenerator.Style.valueOf(styleStr.toUpperCase()); }
        catch (Exception e) { cfg.style = MusicGenerator.Style.POP; }
        cfg.key = (String) input.getOrDefault("key", "C");
        cfg.scale = (String) input.getOrDefault("scale", "major");
        cfg.bpm = ((Number) input.getOrDefault("bpm", 120)).intValue();
        cfg.bars = ((Number) input.getOrDefault("bars", 8)).intValue();
        cfg.beatsPerBar = 4;
        cfg.includeDrums = (Boolean) input.getOrDefault("includeDrums", true);
        cfg.includeChords = (Boolean) input.getOrDefault("includeChords", true);

        byte[] midi = musicGenerator.generate(cfg);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("style", cfg.style.name());
        result.put("key", cfg.key);
        result.put("scale", cfg.scale);
        result.put("bpm", cfg.bpm);
        result.put("bars", cfg.bars);
        result.put("midiBase64", Base64.getEncoder().encodeToString(midi));
        result.put("sizeBytes", midi.length);
        return result;
    }
}
