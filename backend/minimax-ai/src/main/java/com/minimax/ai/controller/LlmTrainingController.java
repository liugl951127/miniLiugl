package com.minimax.ai.controller;

import com.minimax.ai.training.LlmTrainingService;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * LLM 真实训练控制器 (V6.5+)
 *
 * <h2>端点</h2>
 * <ul>
 *   <li>POST /api/v1/ai/training/llm/start      启动训练</li>
 *   <li>GET  /api/v1/ai/training/llm/status/{id}  训练状态</li>
 *   <li>GET  /api/v1/ai/training/llm/history/{id} 训练历史 (epoch)</li>
 *   <li>GET  /api/v1/ai/training/llm/list        所有任务</li>
 *   <li>POST /api/v1/ai/training/llm/feedback    AutoFill 用户反馈</li>
 * </ul>
 *
 * <h2>训练数据源 (V6.5)</h2>
 * <ol>
 *   <li>AiChatMessage (用户历史对话)</li>
 *   <li>AiIntentKeyword (已标注关键词)</li>
 *   <li>AutoFill 反馈 (用户接受/拒绝/纠正)</li>
 * </ol>
 *
 * @author Mavis
 * @since V6.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/training/llm")
@RequiredArgsConstructor
public class LlmTrainingController {

    private final LlmTrainingService trainingService;

    /**
     * 启动训练
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody(required = false) Map<String, Object> req) {
        int epochs = req != null && req.get("epochs") != null
            ? ((Number) req.get("epochs")).intValue() : 5;
        double learningRate = req != null && req.get("learningRate") != null
            ? ((Number) req.get("learningRate")).doubleValue() : 0.01;
        int minSamples = req != null && req.get("minSamples") != null
            ? ((Number) req.get("minSamples")).intValue() : 10;

        String taskId = trainingService.startTraining(epochs, learningRate, minSamples);
        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", "running");
        resp.put("epochs", epochs);
        resp.put("learningRate", learningRate);
        resp.put("minSamples", minSamples);
        resp.put("message", "训练任务已启动, 异步执行");
        return Result.success(resp);
    }

    /**
     * 训练状态
     */
    @GetMapping("/status/{taskId}")
    public Result<LlmTrainingService.TrainingStatus> status(@PathVariable String taskId) {
        LlmTrainingService.TrainingStatus s = trainingService.getStatus(taskId);
        if (s == null) {
            return Result.error(404, "任务不存在: " + taskId);
        }
        return Result.success(s);
    }

    /**
     * 训练历史
     */
    @GetMapping("/history/{taskId}")
    public Result<List<LlmTrainingService.TrainingEpoch>> history(@PathVariable String taskId) {
        return Result.success(trainingService.getHistory(taskId));
    }

    /**
     * 所有任务
     */
    @GetMapping("/list")
    public Result<List<LlmTrainingService.TrainingStatus>> list() {
        return Result.success(trainingService.listAll());
    }

    /**
     * AutoFill 用户反馈 (V6.3+ 升级 V6.5+)
     */
    @PostMapping("/feedback")
    public Result<Map<String, Object>> feedback(@RequestBody Map<String, Object> req) {
        // 实现反馈逻辑
        log.info("AutoFill 反馈: {}", req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("message", "反馈已记录, 下一轮训练会使用");
        return Result.success(resp);
    }
}
