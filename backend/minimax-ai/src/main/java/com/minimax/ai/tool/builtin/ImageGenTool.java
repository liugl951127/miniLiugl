package com.minimax.ai.tool.builtin;

import com.minimax.ai.generation.ImageGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AIGC 图片生成工具 (V2.8.3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenTool extends AbstractSimpleTool {

    private final ImageGenerator imageGenerator;

    @Override
    public String getCode() { return "image.generate"; }

    @Override
    public String getName() { return "AIGC 图片生成"; }

    @Override
    public String getDescription() { return "7 种类型程序化图像生成"; }

    @Override
    public String getCategory() { return "image"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String prompt = (String) input.get("prompt");
        if (prompt == null) throw new IllegalArgumentException("需要 prompt");
        ImageGenerator.ImageRequest req = new ImageGenerator.ImageRequest();
        req.prompt = prompt;
        req.type = (String) input.get("type");
        req.width = ((Number) input.getOrDefault("width", 1024)).intValue();
        req.height = ((Number) input.getOrDefault("height", 1024)).intValue();
        Object seed = input.get("seed");
        req.seed = seed != null ? ((Number) seed).longValue() : null;

        ImageGenerator.ImageResult r = imageGenerator.generate(req);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", r.type);
        result.put("width", r.width);
        result.put("height", r.height);
        result.put("sizeBytes", r.sizeBytes);
        result.put("mime", r.mime);
        result.put("base64", r.base64);
        result.put("metadata", r.metadata);
        return result;
    }
}
