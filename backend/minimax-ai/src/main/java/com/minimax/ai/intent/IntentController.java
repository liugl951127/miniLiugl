package com.minimax.ai.intent;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 客户意图精准预测 REST API (V3.4.1)
 */
@Tag(name = "客户意图预测")
@RestController
@RequestMapping("/api/v1/ai/intent")
@RequiredArgsConstructor
public class IntentController {

    private final IntentPredictionService service;

    @Operation(summary = "预测意图")
    @PostMapping("/predict")
    public Result<IntentPredictionService.IntentPrediction> predict(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        return Result.ok(service.predict(text));
    }

    @Operation(summary = "批量预测")
    @PostMapping("/predict/batch")
    public Result<List<IntentPredictionService.IntentPrediction>> predictBatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) body.get("texts");
        List<IntentPredictionService.IntentPrediction> results = new ArrayList<>();
        for (String t : texts) results.add(service.predict(t));
        return Result.ok(results);
    }

    @Operation(summary = "动态添加关键词")
    @PostMapping("/keyword")
    public Result<Void> addKeyword(@RequestBody Map<String, Object> body) {
        String intent = (String) body.get("intent");
        String keyword = (String) body.get("keyword");
        double weight = ((Number) body.getOrDefault("weight", 5.0)).doubleValue();
        service.addKeyword(intent, keyword, weight);
        return Result.ok();
    }

    @Operation(summary = "列出意图分类")
    @GetMapping("/list")
    public Result<Set<String>> listIntents() {
        return Result.ok(service.listIntents());
    }
}
