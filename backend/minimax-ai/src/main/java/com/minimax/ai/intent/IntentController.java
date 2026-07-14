package com.minimax.ai.intent;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户意图精准预测 REST API (V3.5.6 升级)
 *
 * <h2>新接口</h2>
 * <ul>
 *   <li>POST /predict       : 单条预测 (支持 sessionId 上下文)</li>
 *   <li>POST /predict/batch : 批量预测</li>
 *   <li>POST /keyword       : 动态添加关键词</li>
 *   <li>POST /phrase        : 动态添加短语 (V3.5.6 新增)</li>
 *   <li>GET  /list          : 列出意图分类</li>
 *   <li>POST /context/clear : 清除 session 上下文 (V3.5.6 新增)</li>
 *   <li>GET  /stats         : 算法统计 (V3.5.6 新增)</li>
 *   <li>POST /benchmark     : 内部基准测试 (V3.5.6 新增)</li>
 * </ul>
 */
@Tag(name = "客户意图预测")
@RestController
@RequestMapping("/api/v1/ai/intent")
@RequiredArgsConstructor
public class IntentController {

    private final IntentPredictionService service;
    private final AtomicLong totalPredictions = new AtomicLong(0);
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);

    @Operation(summary = "预测意图 (支持 sessionId 上下文)")
    @PostMapping("/predict")
    public Result<IntentPredictionService.IntentPrediction> predict(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String sessionId = body.get("sessionId");
        long t0 = System.nanoTime();
        IntentPredictionService.IntentPrediction result = service.predict(text, sessionId);
        long cost = System.nanoTime() - t0;
        totalPredictions.incrementAndGet();
        totalLatencyNanos.addAndGet(cost);
        return Result.ok(result);
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

    @Operation(summary = "动态添加短语 (V3.5.6 新增)")
    @PostMapping("/phrase")
    public Result<Void> addPhrase(@RequestBody Map<String, Object> body) {
        String intent = (String) body.get("intent");
        String phrase = (String) body.get("phrase");
        double weight = ((Number) body.getOrDefault("weight", 10.0)).doubleValue();
        service.addPhrase(intent, phrase, weight);
        return Result.ok();
    }

    @Operation(summary = "列出意图分类")
    @GetMapping("/list")
    public Result<Set<String>> listIntents() {
        return Result.ok(service.listIntents());
    }

    @Operation(summary = "清除 session 上下文 (V3.5.6 新增)")
    @PostMapping("/context/clear")
    public Result<Void> clearContext(@RequestBody Map<String, String> body) {
        service.clearContext(body.get("sessionId"));
        return Result.ok();
    }

    @Operation(summary = "算法统计 (V3.5.6 新增)")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        long n = totalPredictions.get();
        long totalNs = totalLatencyNanos.get();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("algorithm", service.getAlgorithmVersion());
        stats.put("totalPredictions", n);
        stats.put("avgLatencyMs", n > 0 ? (totalNs / 1_000_000.0 / n) : 0);
        stats.put("totalIntents", service.listIntents().size());
        return Result.ok(stats);
    }

    @Operation(summary = "内部基准测试 (V3.5.6 新增)")
    @PostMapping("/benchmark")
    public Result<Map<String, Object>> benchmark(@RequestBody(required = false) Map<String, Object> body) {
        int n = body != null && body.get("n") instanceof Number ? ((Number) body.get("n")).intValue() : 100;
        // 100 条标准测试集
        String[][] cases = {
            {"查询一下订单状态", "query"},
            {"我想买 10 台服务器", "order"},
            {"我要退款, 差评, 紧急!", "complaint"},
            {"紧急! 马上修复!!", "complaint"},
            {"13800138000 邮箱 test@example.com 支付 100 元, 明天发货", "pay"},
            {"非常棒, 谢谢!", "feedback"},
            {"我要付款 100 元", "pay"},
            {"查询订单状态", "query"},
            {"怎么登录?", "consult"},
            {"我要注册账号", "register"},
            {"帮我取消订单", "cancel"},
            {"退货!", "complaint"},
            {"购买 5 个 iPhone", "order"},
            {"不满意, 退款", "complaint"},
            {"非常感谢, 帮大忙了", "feedback"},
            {"dingdan zhuangtai", "query"},  // 拼音
            {"i want refund", "complaint"},  // 英文
            {"无法登录怎么办?", "login"},
            {"帮我看看数据", "query"},
            {"下单 100 元", "order"}
        };
        int correct = 0, total = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            for (String[] c : cases) {
                IntentPredictionService.IntentPrediction r = service.predict(c[0]);
                boolean ok = c[1].equals(r.getIntent());
                if (ok) correct++;
                total++;
                if (i == 0) {
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("text", c[0]);
                    d.put("expected", c[1]);
                    d.put("actual", r.getIntent());
                    d.put("confidence", r.getConfidence());
                    d.put("ok", ok);
                    details.add(d);
                }
            }
        }
        long cost = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iterations", n);
        result.put("totalCases", total);
        result.put("correct", correct);
        result.put("accuracy", (double) correct / total);
        result.put("avgLatencyMs", cost / 1_000_000.0 / total);
        result.put("algorithm", service.getAlgorithmVersion());
        result.put("sampleDetails", details.subList(0, Math.min(10, details.size())));
        return Result.ok(result);
    }
}
