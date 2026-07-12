package com.minimax.ai.controller;

import com.minimax.ai.generation.IntentService;
import com.minimax.ai.pipeline.PipelineExecutor;
import com.minimax.ai.pipeline.PipelineExecutor.PipelineRequest;
import com.minimax.ai.pipeline.PipelineExecutor.PipelineResult;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.config.PipelineConfig.ComputeMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Pipeline API 端点 (V2.8.5)
 *
 * <h3>端点</h3>
 * <ul>
 *   <li>POST /api/ai/pipeline/execute - 执行完整 13 阶段 pipeline</li>
 *   <li>GET  /api/ai/pipeline/config - 查看配置</li>
 *   <li>POST /api/ai/pipeline/config - 修改配置 (热加载)</li>
 *   <li>POST /api/ai/pipeline/config/compute-mode - 切换 CPU/GPU</li>
 *   <li>POST /api/ai/pipeline/intent/reload - 重新加载关键词</li>
 *   <li>GET  /api/ai/pipeline/intent/stats - 关键词缓存统计</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/pipeline")
@RequiredArgsConstructor
@Tag(name = "AI Pipeline", description = "13 阶段 AI 推理流水线")
public class PipelineController {

    private final PipelineExecutor pipelineExecutor;
    private final IntentService intentService;

    /**
     * 执行 pipeline
     */
    @PostMapping("/execute")
    @Operation(summary = "执行完整 13 阶段 AI pipeline")
    public PipelineResult execute(@RequestBody PipelineRequest request) {
        if (request.sessionId == null) {
            request.sessionId = "session-" + System.currentTimeMillis();
        }
        return pipelineExecutor.execute(request);
    }

    /**
     * 查看当前配置
     */
    @GetMapping("/config")
    @Operation(summary = "查看流水线配置")
    public PipelineConfig.Data getConfig() {
        return PipelineConfig.snapshot();
    }

    /**
     * 修改配置 (热加载)
     */
    @PostMapping("/config")
    @Operation(summary = "修改流水线配置")
    public PipelineConfig.Data updateConfig(@RequestBody Map<String, Object> updates) {
        if (updates.containsKey("temperature")) {
            PipelineConfig.TEMPERATURE = ((Number) updates.get("temperature")).floatValue();
        }
        if (updates.containsKey("topK")) {
            PipelineConfig.TOP_K = ((Number) updates.get("topK")).intValue();
        }
        if (updates.containsKey("topP")) {
            PipelineConfig.TOP_P = ((Number) updates.get("topP")).floatValue();
        }
        if (updates.containsKey("maxGenerateTokens")) {
            PipelineConfig.MAX_GENERATE_TOKENS = ((Number) updates.get("maxGenerateTokens")).intValue();
        }
        if (updates.containsKey("maxHistoryTurns")) {
            PipelineConfig.MAX_HISTORY_TURNS = ((Number) updates.get("maxHistoryTurns")).intValue();
        }
        if (updates.containsKey("ragTopK")) {
            PipelineConfig.RAG_TOP_K = ((Number) updates.get("ragTopK")).intValue();
        }
        if (updates.containsKey("ragSimilarityThreshold")) {
            PipelineConfig.RAG_SIMILARITY_THRESHOLD = ((Number) updates.get("ragSimilarityThreshold")).doubleValue();
        }
        if (updates.containsKey("batchSize")) {
            PipelineConfig.BATCH_SIZE = ((Number) updates.get("batchSize")).intValue();
        }
        if (updates.containsKey("enableKvCache")) {
            PipelineConfig.ENABLE_KV_CACHE = (Boolean) updates.get("enableKvCache");
        }
        if (updates.containsKey("enableInt8Quant")) {
            PipelineConfig.ENABLE_INT8_QUANT = (Boolean) updates.get("enableInt8Quant");
        }
        return PipelineConfig.snapshot();
    }

    /**
     * 切换计算模式
     */
    @PostMapping("/config/compute-mode")
    @Operation(summary = "切换 CPU/GPU 模式")
    public Map<String, Object> switchComputeMode(@RequestParam String mode) {
        ComputeMode m;
        try {
            m = ComputeMode.valueOf(mode.toUpperCase());
        } catch (Exception e) {
            return Map.of("error", "Invalid mode: " + mode + " (use CPU/GPU/AUTO)");
        }
        PipelineConfig.setComputeMode(m);
        return Map.of(
                "computeMode", m.name(),
                "activeDevice", PipelineConfig.getActiveDevice(),
                "resolvedDevice", PipelineConfig.resolveDevice()
        );
    }

    /**
     * 重载关键词
     */
    @PostMapping("/intent/reload")
    @Operation(summary = "重新加载关键词 (从 DB)")
    public Map<String, Object> reloadIntent() {
        intentService.reload();
        return intentService.stats();
    }

    /**
     * 关键词缓存统计
     */
    @GetMapping("/intent/stats")
    @Operation(summary = "关键词缓存统计")
    public Map<String, Object> intentStats() {
        return intentService.stats();
    }
}
