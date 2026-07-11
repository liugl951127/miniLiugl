package com.minimax.ai.controller;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import com.minimax.ai.training.TrainerService;
import com.minimax.ai.training.TrainingTracker;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 训练可视化 API (V2.7.5)
 *
 * 端点:
 *   POST /api/ai/training/start     启动训练任务
 *   GET  /api/ai/training/tasks     所有任务
 *   GET  /api/ai/training/tasks/{id} 任务详情
 *   GET  /api/ai/training/tasks/{id}/history  历史指标
 *   DELETE /api/ai/training/tasks/{id} 删除任务
 *   POST /api/ai/training/demo    演示模式 (用小语料立刻跑)
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingTracker tracker;
    private final TrainerService trainerService;
    private final ChineseTokenizer tokenizer;

    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody Map<String, Object> req) {
        String name = (String) req.getOrDefault("name", "unnamed");
        String modelType = (String) req.getOrDefault("model", "mini-transformer");
        int epochs = ((Number) req.getOrDefault("epochs", 5)).intValue();
        double lr = ((Number) req.getOrDefault("learningRate", 0.01)).doubleValue();

        // 默认语料
        List<String> corpus;
        if (req.get("corpus") instanceof List<?> c) {
            corpus = ((List<?>) c).stream().map(Object::toString).toList();
        } else {
            corpus = defaultCorpus();
        }
        final List<String> finalCorpus = corpus;

        final String taskId = tracker.createTask(name, modelType, epochs, req.toString());

        // 同步训练 (简化); 实际可改成 @Async
        MiniTransformer model = new MiniTransformer(tokenizer.getVocabSize(), 32, 2, 2, 64);
        new Thread(() -> trainerService.train(taskId, model, finalCorpus, epochs, lr), "train-" + taskId).start();

        return Result.ok(Map.of("taskId", taskId, "name", name, "status", "PENDING"));
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(tracker.listAll().stream().map(TrainingTracker.TaskInfo::toMap).toList());
    }

    @GetMapping("/tasks/{id}")
    public Result<Map<String, Object>> get(@PathVariable String id) {
        TrainingTracker.TaskInfo t = tracker.get(id);
        if (t == null) return Result.fail("任务不存在: " + id);
        return Result.ok(t.toMap());
    }

    @GetMapping("/tasks/{id}/history")
    public Result<Map<String, Object>> history(@PathVariable String id) {
        TrainingTracker.TaskInfo t = tracker.get(id);
        if (t == null) return Result.fail("任务不存在: " + id);
        List<TrainingTracker.MetricPoint> h = tracker.getHistory(id);
        List<Map<String, Object>> points = h.stream().map(TrainingTracker.MetricPoint::toMap).toList();
        // 同步算 EMA
        List<Double> losses = h.stream().map(p -> p.loss).toList();
        List<Double> ema = TrainingTracker.ema(losses, 0.1);
        return Result.ok(Map.of(
                "task", t.toMap(),
                "points", points,
                "emaLoss", ema,
                "minLoss", losses.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                "maxLoss", losses.stream().mapToDouble(Double::doubleValue).max().orElse(0),
                "finalLoss", losses.isEmpty() ? 0 : losses.get(losses.size() - 1)
        ));
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Boolean> delete(@PathVariable String id) {
        return Result.ok(tracker.remove(id));
    }

    /**
     * 演示模式: 用极小语料 + 2 epoch 跑一次, 立即返回任务 ID
     */
    @PostMapping("/demo")
    public Result<Map<String, Object>> demo() {
        String taskId = tracker.createTask("训练演示", "mini-transformer", 5, "demo");
        List<String> corpus = List.of(
                "深度学习是机器学习的一个分支",
                "Transformer 是当前主流架构",
                "MiniMax 自研 AI 平台",
                "训练可视化 TensorBoard 风格",
                "Spring Boot + MyBatis-Plus 后端"
        );
        MiniTransformer model = new MiniTransformer(tokenizer.getVocabSize(), 32, 2, 2, 64);
        new Thread(() -> trainerService.train(taskId, model, corpus, 5, 0.05), "train-demo").start();
        return Result.ok(Map.of("taskId", taskId, "status", "PENDING", "name", "训练演示"));
    }

    private List<String> defaultCorpus() {
        return List.of(
                "MiniMax 是一个企业级 AI 平台",
                "支持多模态分析, 智能问答, 报告生成",
                "训练可视化帮助理解模型收敛过程",
                "Cross-Entropy Loss 衡量预测与真实分布差异",
                "Perplexity 是语言模型常用评价指标"
        );
    }
}
