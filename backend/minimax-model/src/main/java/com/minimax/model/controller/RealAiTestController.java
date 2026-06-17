package com.minimax.model.controller;

import com.minimax.common.result.Result;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.provider.ModelProviderFactory;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * 真实 AI 对接测试端点 (V4).
 * - /test/ping   健康检查
 * - /test/single 单次非流式
 * - /test/battle 多模型并发对决
 *
 * @since 2026-06
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class RealAiTestController {

    private final ModelService modelService;
    private final ModelProviderFactory providerFactory;
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "minimax-model");
        data.put("status", "UP");
        data.put("ts", System.currentTimeMillis());
        data.put("mockMode", isMockMode());
        return Result.ok(data);
    }

    private boolean isMockMode() {
        try {
            return providerFactory != null;
        } catch (Exception e) { return true; }
    }

    @PostMapping("/single")
    @SuppressWarnings("unchecked")
    public Result<ChatResponse> testSingle(@RequestBody Map<String, Object> body) {
        String modelCode = (String) body.getOrDefault("model", "mock");
        String prompt = (String) body.getOrDefault("prompt", "");
        String sysPrompt = (String) body.get("systemPrompt");
        Double temperature = body.get("temperature") != null
                ? ((Number) body.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = body.get("maxTokens") != null
                ? ((Number) body.get("maxTokens")).intValue() : 1024;

        if (prompt == null || prompt.isBlank()) {
            return Result.fail(400, "prompt 不能为空");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (sysPrompt != null && !sysPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", sysPrompt));
        }
        messages.add(Map.of("role", "user", "content", prompt));

        ChatRequest req = new ChatRequest();
        req.setModel(modelCode);
        req.setMessages(messages);
        req.setTemperature(temperature);
        req.setMaxTokens(maxTokens);
        req.setStream(false);

        try {
            ChatResponse resp = modelService.chat(0L, req);
            return Result.ok(resp);
        } catch (Exception e) {
            log.error("testSingle 失败 model={}: {}", modelCode, e.getMessage());
            return Result.fail(500, "调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/battle")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> battle(@RequestBody Map<String, Object> body) {
        String prompt = (String) body.getOrDefault("prompt", "");
        List<String> models = (List<String>) body.getOrDefault("models", List.of(
                "gpt-4o-mini", "MiniMax-Text-01", "qwen-max", "deepseek-chat"));
        String judgeModel = (String) body.getOrDefault("judgeModel", "gpt-4o-mini");

        if (prompt.isBlank()) {
            return Result.fail(400, "prompt 不能为空");
        }

        String battleId = "b_" + System.currentTimeMillis();
        List<CompletableFuture<BattleResult>> futures = new ArrayList<>();

        for (String mc : models) {
            futures.add(CompletableFuture.supplyAsync(() -> doBattleOne(battleId, mc, prompt), pool));
        }

        List<BattleResult> results = new ArrayList<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(120, TimeUnit.SECONDS);
            for (CompletableFuture<BattleResult> f : futures) {
                results.add(f.get());
            }
        } catch (TimeoutException te) {
            log.warn("battle {} timeout, 部分未完成", battleId);
            for (int i = 0; i < futures.size(); i++) {
                if (futures.get(i).isDone()) {
                    try { results.add(futures.get(i).get()); } catch (Exception ignore) {}
                } else {
                    futures.get(i).cancel(true);
                    BattleResult r = new BattleResult();
                    r.modelCode = models.get(i);
                    r.status = "timeout";
                    r.error = "120s 超时未返回";
                    results.add(r);
                }
            }
        } catch (Exception e) {
            return Result.fail(500, "对决异常: " + e.getMessage());
        }

        results.sort((a, b) -> {
            if (!a.status.equals(b.status)) return a.status.equals("ok") ? -1 : 1;
            return Long.compare(a.latencyMs, b.latencyMs);
        });

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("battleId", battleId);
        out.put("prompt", prompt);
        out.put("judgeModel", judgeModel);
        out.put("results", results);
        out.put("winnerAuto", results.stream().filter(r -> "ok".equals(r.status))
                .findFirst().map(r -> r.modelCode).orElse(null));
        return Result.ok(out);
    }

    private BattleResult doBattleOne(String battleId, String modelCode, String prompt) {
        long t0 = System.currentTimeMillis();
        BattleResult r = new BattleResult();
        r.modelCode = modelCode;
        r.battleId = battleId;
        try {
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", prompt)
            );
            ChatRequest req = new ChatRequest();
            req.setModel(modelCode);
            req.setMessages(messages);
            req.setStream(false);
            req.setTemperature(0.7);
            req.setMaxTokens(512);

            ChatResponse resp = modelService.chat(0L, req);
            r.status = "ok";
            r.content = resp.getContent();
            r.promptTokens = resp.getPromptTokens();
            r.completionTokens = resp.getCompletionTokens();
            r.totalTokens = resp.getTotalTokens();
            r.latencyMs = resp.getLatencyMs() != null ? resp.getLatencyMs() : (System.currentTimeMillis() - t0);
            r.finishReason = resp.getFinishReason();
        } catch (Exception e) {
            log.warn("battle 单模型失败: {} - {}", modelCode, e.getMessage());
            r.status = "error";
            r.error = e.getMessage() != null && e.getMessage().length() > 200
                    ? e.getMessage().substring(0, 200) : e.getMessage();
            r.latencyMs = System.currentTimeMillis() - t0;
        }
        return r;
    }

    public static class BattleResult {
        public String battleId;
        public String modelCode;
        public String status;
        public String content;
        public String error;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
        public long latencyMs;
        public String finishReason;
    }
}
