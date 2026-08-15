package com.minimax.ai.controller;

import com.minimax.ai.generation.IntentService;
import com.minimax.ai.entity.AiIntentKeyword;
import com.minimax.ai.mapper.AiIntentKeywordMapper;
import com.minimax.ai.training.LlmTrainingService;
import com.minimax.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Intent 真实业务控制器 (V6.5+)
 * 替换 MissingAiController 的 /ai/intent/* 兜底路由
 *
 * @author Mavis
 * @since V6.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/intent")
@RequiredArgsConstructor
public class AiIntentRealController {

    private final IntentService intentService;
    private final AiIntentKeywordMapper keywordMapper;
    private final LlmTrainingService llmTrainingService;

    /**
     * 识别意图 (V6.5+ 真业务)
     */
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        String sessionId = (String) body.get("sessionId");
        IntentService.RecognitionResult r = intentService.recognizeWithDetails(text, sessionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("intent", r.getIntent());
        resp.put("confidence", r.getConfidence());
        resp.put("model", r.getModel());
        resp.put("alternatives", r.getAlternatives());
        return Result.ok(resp);
    }

    /**
     * 列出意图关键词
     */
    @GetMapping("/keywords")
    public Result<List<AiIntentKeyword>> listKeywords(
            @RequestParam(required = false) String intent) {
        QueryWrapper<AiIntentKeyword> qw = new QueryWrapper<>();
        if (intent != null) qw.eq("intent", intent);
        qw.orderByDesc("weight").last("LIMIT 200");
        return Result.ok(keywordMapper.selectList(qw));
    }

    /**
     * 添加关键词
     */
    @PostMapping("/keywords")
    public Result<AiIntentKeyword> addKeyword(@RequestBody AiIntentKeyword kw) {
        kw.setId(null);
        keywordMapper.insert(kw);
        return Result.ok(kw);
    }

    /**
     * 训练意图 (V6.5+ 触发 LLM 训练)
     */
    @PostMapping("/train")
    public Result<Map<String, Object>> trainIntent(@RequestBody(required = false) Map<String, Object> req) {
        int epochs = req != null && req.get("epochs") != null
            ? ((Number) req.get("epochs")).intValue() : 5;
        String taskId = llmTrainingService.startTraining(epochs, 0.01, 10);
        return Result.ok(Map.of(
            "taskId", taskId,
            "status", "running",
            "epochs", epochs,
            "message", "意图训练已启动 (V6.5+ LLM 真实数据)"
        ));
    }
}
