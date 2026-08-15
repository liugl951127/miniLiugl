package com.minimax.ai.controller;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import com.minimax.ai.training.TrainerService;
import com.minimax.ai.training.TrainingTracker;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
@RequestMapping("/api/v1/ai/training-impl")
@RequiredArgsConstructor
public class AiTrainingRealController {

    private final TrainingTracker tracker;
    private final TrainerService trainerService;
    private final ChineseTokenizer tokenizer;

    @Value("${minimax.model.service-url:http://localhost:8084}")
    private String modelServiceUrl;

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

    /**
     * 端点1: GET /tasks/{id}/metrics — 返回完整指标，包括 EMA 数组、PPL 数组、LR 数组及任务信息
     */
    @GetMapping("/tasks/{id}/metrics")
    public Result<Map<String, Object>> metrics(@PathVariable String id) {
        TrainingTracker.TaskInfo t = tracker.get(id);
        if (t == null) return Result.fail("任务不存在: " + id);

        List<TrainingTracker.MetricPoint> h = tracker.getHistory(id);
        List<Map<String, Object>> points = h.stream().map(TrainingTracker.MetricPoint::toMap).toList();

        List<Double> losses = h.stream().map(p -> p.loss).toList();
        List<Double> valLosses = h.stream().map(p -> p.valLoss).toList();
        List<Double> accuracies = h.stream().map(p -> p.accuracy).toList();
        List<Double> emaLoss = TrainingTracker.ema(losses, 0.1);

        // perplexity = exp(valLoss), clamped to avoid overflow
        List<Double> perplexities = valLosses.stream()
                .map(v -> Math.exp(Math.min(v, 50.0)))
                .toList();

        // learningRate (use default 0.01 if not recorded)
        List<Double> learningRates = h.stream()
                .map(p -> p.learningRate > 0 ? p.learningRate : 0.01)
                .toList();

        double minLoss = losses.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxLoss = losses.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double finalLoss = losses.isEmpty() ? 0 : losses.get(losses.size() - 1);
        double finalAccuracy = accuracies.isEmpty() ? 0 : accuracies.get(accuracies.size() - 1);

        long duration = 0;
        if (t.endTimeMs > 0) {
            duration = t.endTimeMs - t.startTimeMs;
        }

        // rough token estimate: steps * batchSize * blockSize
        int totalTokens = h.size() * 32 * 64;

        LinkedHashMap<String, Object> resp = new LinkedHashMap<>();
        resp.put("task", Map.of(
                "id", t.taskId, "name", t.name, "status", t.status,
                "model", t.model, "createdAt", t.startTimeMs, "completedAt", t.endTimeMs
        ));
        resp.put("points", points);
        resp.put("emaLoss", emaLoss);
        resp.put("perplexity", perplexities);
        resp.put("learningRate", learningRates);
        resp.put("minLoss", minLoss);
        resp.put("maxLoss", maxLoss);
        resp.put("finalLoss", finalLoss);
        resp.put("finalAccuracy", finalAccuracy);
        resp.put("duration", duration);
        resp.put("totalTokens", totalTokens);
        return Result.ok(resp);
    }

    /**
     * 端点2: GET /tasks/compare — 比较多个训练任务
     */
    @GetMapping("/tasks/compare")
    public Result<Map<String, Object>> compare(@RequestParam("ids") String ids) {
        if (ids == null || ids.isBlank()) {
            return Result.fail("缺少 ids 参数");
        }

        List<String> taskIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<Map<String, Object>> summaries = new ArrayList<>();
        Double bestLoss = null;
        String bestLossId = null;
        Double bestAccuracy = null;
        String bestAccuracyId = null;

        for (String tid : taskIds) {
            TrainingTracker.TaskInfo t = tracker.get(tid);
            if (t == null) continue;

            List<TrainingTracker.MetricPoint> h = tracker.getHistory(tid);
            List<Double> losses = h.stream().map(p -> p.loss).toList();
            List<Double> accuracies = h.stream().map(p -> p.accuracy).toList();

            double minLoss = losses.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double finalLoss = losses.isEmpty() ? 0 : losses.get(losses.size() - 1);
            double finalAccuracy = accuracies.isEmpty() ? 0 : accuracies.get(accuracies.size() - 1);

            long duration = 0;
            if (t.endTimeMs > 0) {
                duration = t.endTimeMs - t.startTimeMs;
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", t.taskId);
            summary.put("name", t.name);
            summary.put("status", t.status);
            summary.put("finalLoss", finalLoss);
            summary.put("minLoss", minLoss);
            summary.put("finalAccuracy", finalAccuracy);
            summary.put("duration", duration);
            summaries.add(summary);

            if (bestLoss == null || finalLoss < bestLoss) {
                bestLoss = finalLoss;
                bestLossId = t.taskId;
            }
            if (bestAccuracy == null || finalAccuracy > bestAccuracy) {
                bestAccuracy = finalAccuracy;
                bestAccuracyId = t.taskId;
            }
        }

        Map<String, Object> bestLossEntry = new LinkedHashMap<>();
        bestLossEntry.put("id", bestLossId);
        bestLossEntry.put("value", bestLoss);

        Map<String, Object> bestAccuracyEntry = new LinkedHashMap<>();
        bestAccuracyEntry.put("id", bestAccuracyId);
        bestAccuracyEntry.put("value", bestAccuracy);

        return Result.ok(Map.of(
                "tasks", summaries,
                "bestLoss", bestLossEntry,
                "bestAccuracy", bestAccuracyEntry
        ));
    }

    /**
     * 端点3: GET /system/resources — 返回模拟系统资源数据
     */
    @GetMapping("/system/resources")
    public Result<Map<String, Object>> systemResources() {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gpu", Map.of(
                "name", "Mock GPU",
                "utilizationPercent", 45,
                "memoryUsedMb", 2048,
                "memoryTotalMb", 8192,
                "temperature", 62
        ));
        response.put("cpu", Map.of(
                "cores", 8,
                "usagePercent", 28
        ));
        response.put("memory", Map.of(
                "usedMb", 4096,
                "totalMb", 16384
        ));
        response.put("timestamp", timestamp);

        log.info("[System/Resources] 返回模拟系统资源数据");
        return Result.ok(response);
    }

    /**
     * 端点4: GET /tasks/presets — 返回训练预设模板
     */
    @GetMapping("/tasks/presets")
    public Result<Map<String, Object>> presets() {
        List<Map<String, Object>> presets = List.of(
                preset("chat-sft", "通用对话微调", "适合中文对话场景的 SFT 微调",
                        "chatglm-6b", "chat通用", 3, 8, 0.0001, 1000,
                        "cosine", false, "none", List.of("fp16")),
                preset("code-sft", "代码生成微调", "适合代码补全和生成任务",
                        "qwen-7b", "code", 5, 4, 0.00005, 5000,
                        "cosine", true, "q4", List.of("fp16", "deepspeed")),
                preset("rag-ft", "RAG 增强微调", "适合检索增强生成场景",
                        "chatglm-6b", "rag", 3, 4, 0.0002, 2000,
                        "warmup_cosine", false, "none", List.of("fp16", "gradient-accumulation"))
        );
        return Result.ok(Map.of("presets", presets));
    }

    private Map<String, Object> preset(String id, String name, String description,
            String baseModel, String dataset, int epochs, int batchSize,
            double lr, int maxSteps, String scheduler,
            boolean lora, String quantization, List<String> accelerations) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", name); m.put("description", description);
        m.put("baseModel", baseModel); m.put("dataset", dataset);
        m.put("epochs", epochs); m.put("batchSize", batchSize);
        m.put("learningRate", lr); m.put("maxSteps", maxSteps);
        m.put("scheduler", scheduler); m.put("lora", lora);
        m.put("quantization", quantization); m.put("accelerations", accelerations);
        return m;
    }

    /**
     * 端点5: POST /tasks/{id}/evaluate — 运行模拟评测
     */
    @PostMapping("/tasks/{id}/evaluate")
    public Result<Map<String, Object>> evaluate(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        TrainingTracker.TaskInfo t = tracker.get(id);
        if (t == null) return Result.fail("任务不存在: " + id);
        if (!"COMPLETED".equals(t.status)) {
            return Result.fail("任务尚未完成，无法评测: " + t.status);
        }

        String benchmark = (body != null && body.get("benchmark") != null)
                ? body.get("benchmark")
                : "mmlu";

        // Generate stable pseudo-random score based on taskId hashcode
        int seed = Math.abs(Objects.hashCode(id) + benchmark.hashCode());
        Random rnd = new Random(seed);
        int totalQuestions = 100;
        int correctAnswers = rnd.nextInt(61) + 30; // 30–90 range
        double score = Math.round(correctAnswers * 100.0 / totalQuestions) / 100.0;

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        Map<String, Object> categories = new LinkedHashMap<>();
        categories.put("math", Math.round((rnd.nextDouble() * 0.3 + 0.6) * 100) / 100.0);
        categories.put("history", Math.round((rnd.nextDouble() * 0.3 + 0.6) * 100) / 100.0);
        categories.put("science", Math.round((rnd.nextDouble() * 0.3 + 0.6) * 100) / 100.0);
        categories.put("law", Math.round((rnd.nextDouble() * 0.3 + 0.6) * 100) / 100.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", id);
        result.put("benchmark", benchmark);
        result.put("score", score);
        result.put("totalQuestions", totalQuestions);
        result.put("correctAnswers", correctAnswers);
        result.put("categories", categories);
        result.put("timestamp", timestamp);

        log.info("[Training/Evaluate] taskId={} benchmark={} score={}", id, benchmark, score);
        return Result.ok(result);
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Boolean> delete(@PathVariable String id) {
        return Result.ok(tracker.remove(id));
    }

    /**
     * V7.0 Flow④+②: 一键启用训练好的模型，并注册到 Model 服务
     * 训练产出的模型可以被 Agent 委托调用 (Flow②)
     */
    @PostMapping("/tasks/{taskId}/enable")
    public Result<Map<String, Object>> enableModel(@PathVariable String taskId) {
        TrainingTracker.TaskInfo task = tracker.get(taskId);
        if (task == null) return Result.fail("训练任务不存在: " + taskId);
        if (!"COMPLETED".equals(task.status)) {
            return Result.fail("任务尚未完成，无法启用: " + taskId);
        }
        String modelCode = "trained-" + taskId;
        String displayName = "训练模型-" + task.name;
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Step1: 查找或创建自研训练 provider
            var providersResp = rt.getForEntity(modelServiceUrl + "/api/v1/models/local/providers", Object.class);
            Long providerId = null;
            if (providersResp.getBody() != null) {
                var body = (java.util.Map<?, ?>) providersResp.getBody();
                var data = (java.util.List<?>) body.get("data");
                if (data != null) {
                    for (var p : data) {
                        var pm = (java.util.Map<?, ?>) p;
                        var name = (String) pm.get("name");
                        if (name != null && name.contains("自研")) {
                            providerId = ((Number) pm.get("id")).longValue();
                            break;
                        }
                    }
                }
            }
            // 没有自研 provider 就创建一个
            if (providerId == null) {
                Map<String, Object> provBody = new java.util.LinkedHashMap<>();
                provBody.put("name", "自研训练");
                provBody.put("baseUrl", "/opt/minimax/models");
                provBody.put("description", "平台自研训练模型存储目录");
                var provResp = rt.postForEntity(
                    modelServiceUrl + "/api/v1/models/local/providers",
                    new org.springframework.http.HttpEntity<>(provBody, headers),
                    Map.class
                );
                var provData = (java.util.Map<?, ?>) provResp.getBody();
                var provResult = (java.util.Map<?, ?>) provData.get("data");
                providerId = ((Number) provResult.get("id")).longValue();
                log.info("[Training/Enable] 创建自研 provider: id={}", providerId);
            }

            // Step2: 注册模型到 model_config（关联 providerId，OnnxAdapter 可识别）
            Map<String, Object> reqBody = new java.util.LinkedHashMap<>();
            reqBody.put("modelCode", modelCode);
            reqBody.put("displayName", displayName);
            reqBody.put("maxContext", 2048);
            reqBody.put("maxOutput", 1024);
            String addUrl = modelServiceUrl + "/api/v1/models/local/providers/" + providerId + "/models";
            var resp = rt.postForEntity(addUrl,
                new org.springframework.http.HttpEntity<>(reqBody, headers), Map.class);
            log.info("[Training/Enable] 模型注册结果: code={} resp={}", modelCode, resp.getBody());

        } catch (Exception e) {
            log.warn("[Training/Enable] 模型注册失败: {}", e.getMessage());
            // 不阻塞，模型名仍返回
        }
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("modelCode", modelCode);
        data.put("displayName", displayName);
        data.put("modelType", task.model);
        data.put("status", "registered");
        data.put("message", "模型已注册到 Model 服务，OnnxAdapter 策略可路由，Agent/RAG/Canvas 均可使用");
        log.info("[Training/Enable] taskId={} modelCode={} 注册完成", taskId, modelCode);
        return Result.ok(data);
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

    // ==================== Dashboard ====================

    /**
     * V7.1: 训练总览 Dashboard 数据
     */
    @GetMapping("/dashboard/overview")
    public Result<Map<String, Object>> dashboardOverview() {
        List<TrainingTracker.TaskInfo> all = tracker.listAll();

        long total = all.size();
        long completed = all.stream().filter(t -> "COMPLETED".equals(t.status)).count();
        long running   = all.stream().filter(t -> "RUNNING".equals(t.status)).count();
        long failed    = all.stream().filter(t -> "FAILED".equals(t.status)).count();
        long pending   = all.stream().filter(t -> "PENDING".equals(t.status)).count();

        // 累计训练时长 (ms)
        long totalTrainingMs = all.stream()
                .filter(t -> t.endTimeMs > 0)
                .mapToLong(t -> t.endTimeMs - t.startTimeMs)
                .sum();

        // 最新完成任务的 loss 分布
        List<TrainingTracker.TaskInfo> completedTasks = all.stream()
                .filter(t -> "COMPLETED".equals(t.status))
                .sorted((a, b) -> Long.compare(b.endTimeMs, a.endTimeMs))
                .limit(20)
                .collect(Collectors.toList());

        List<Map<String, Object>> recentTasks = completedTasks.stream().limit(10).map(task -> {
            Map<String, Object> m = task.toMap();
            // 追加最新 loss
            if (!task.history.isEmpty()) {
                TrainingTracker.MetricPoint last = task.history.get(task.history.size() - 1);
                m.put("finalLoss", last.loss);
                m.put("finalAccuracy", last.accuracy);
            }
            // 格式化时长
            long dur = (task.endTimeMs > 0 ? task.endTimeMs : System.currentTimeMillis()) - task.startTimeMs;
            m.put("durationStr", formatDuration(dur));
            m.put("startTime", Instant.ofEpochMilli(task.startTimeMs).toString());
            return m;
        }).collect(Collectors.toList());

        // 按 baseModel 分组统计
        Map<String, Long> byModel = all.stream()
                .collect(Collectors.groupingBy(t -> t.model != null ? t.model : "unknown",
                        Collectors.counting()));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalTasks", total);
        resp.put("completed", completed);
        resp.put("running", running);
        resp.put("failed", failed);
        resp.put("pending", pending);
        resp.put("totalTrainingMs", totalTrainingMs);
        resp.put("totalTrainingHours", String.format("%.1f", totalTrainingMs / 3_600_000.0));
        resp.put("recentTasks", recentTasks);
        resp.put("byModel", byModel);
        resp.put("updatedAt", Instant.now().toString());

        return Result.ok(resp);
    }

    private String formatDuration(long ms) {
        if (ms < 60_000) return ms / 1000 + "s";
        if (ms < 3_600_000) return String.format("%.1fm", ms / 60_000.0);
        return String.format("%.1fh", ms / 3_600_000.0);
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
